package com.pixelquest.app.study.chat.data

import android.content.Context
import com.pixelquest.app.BuildConfig
import com.pixelquest.app.study.chat.data.remote.ChatFilesRemote
import com.pixelquest.app.study.chat.data.remote.FileCall
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import com.pixelquest.app.study.chat.domain.UploadChecks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** How opening a shared file went. */
sealed interface OpenedFile {
    /** Saved in the app's own storage, its bytes the ones the server checked. */
    data class Saved(val file: File, val mime: String) : OpenedFile

    /** Not the player's to open (they left the party, or it was deleted). */
    data object NotAllowed : OpenedFile

    data object Offline : OpenedFile

    /** What came wasn't what the server had checked (its size or hash): not kept. */
    data object Corrupted : OpenedFile
}

/** Fetches a shared file for the player to open: Android and HTTP in the app, a fake in tests. */
fun interface ChatFileFetcher {
    suspend fun fetch(file: ChatFile): OpenedFile
}

/**
 * Opens a shared file (docs/study-chat.md, How a file is shared, step 5): a 60-second URL from
 * get-download-url (membership checked again, now), then its bytes into the app's own cache,
 * study-files/<file id>/<its cleaned name>, the one folder its FileProvider shares. What arrives
 * must be the size and hash the server recorded when it checked the file; anything else isn't
 * kept. Only the player's tap starts this, and only the system viewer they pick opens it.
 */
@Singleton
class HttpChatFileFetcher(
    private val context: Context,
    private val remote: ChatFilesRemote,
    private val supabaseUrl: String,
    private val http: OkHttpClient
) : ChatFileFetcher {
    @Inject
    constructor(@ApplicationContext context: Context, remote: ChatFilesRemote) : this(
        context, remote, BuildConfig.SUPABASE_URL,
        OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    )

    override suspend fun fetch(file: ChatFile): OpenedFile = withContext(Dispatchers.IO) {
        val link = when (val asked = remote.downloadUrl(file.id)) {
            is FileCall.Ok -> asked.data
            is FileCall.Refused, FileCall.SignedOut -> return@withContext OpenedFile.NotAllowed
            FileCall.Offline -> return@withContext OpenedFile.Offline
        }
        // Only the project's own storage, at a signed path: never anywhere else.
        if (!link.path.startsWith("/storage/v1/object/sign/study-files/")) return@withContext OpenedFile.NotAllowed
        val folder = File(context.cacheDir, "study-files/${file.id.filter { it.isLetterOrDigit() || it == '-' }}").apply { mkdirs() }
        val name = UploadChecks.cleanName(link.name) ?: "file"
        val target = File(folder, name)
        // The name can't lead out of its folder, whatever it says.
        if (target.canonicalFile.parentFile != folder.canonicalFile) return@withContext OpenedFile.Corrupted
        val part = File(folder, "$name.part")
        try {
            http.newCall(Request.Builder().url(supabaseUrl.trim().trimEnd('/') + link.path).build()).execute().use { response ->
                if (response.code == 400 || response.code == 403 || response.code == 404) return@withContext OpenedFile.NotAllowed
                if (!response.isSuccessful) return@withContext OpenedFile.Offline
                val digest = MessageDigest.getInstance("SHA-256")
                var size = 0L
                response.body!!.byteStream().use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            size += read
                            if (size > StudyChatConfig.FILE_MAX_BYTES) {
                                part.delete()
                                return@withContext OpenedFile.Corrupted
                            }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                    }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (size != file.sizeBytes || (file.sha256 != null && hash != file.sha256)) {
                    part.delete()
                    return@withContext OpenedFile.Corrupted
                }
            }
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            OpenedFile.Saved(target, file.mime)
        } catch (e: IOException) {
            part.delete()
            OpenedFile.Offline
        }
    }
}
