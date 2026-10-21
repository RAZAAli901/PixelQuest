package com.pixelquest.app.study.chat.data.remote

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.domain.ai.AiAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** How a call to a file function went. */
sealed interface FileCall<out T> {
    data class Ok<T>(val data: T) : FileCall<T>

    /** The server said no, with its reason ("too_large", "not_a_member", ...). */
    data class Refused(val error: String) : FileCall<Nothing>

    /** No connection, or the server couldn't be reached: worth trying again later. */
    data object Offline : FileCall<Nothing>

    data object SignedOut : FileCall<Nothing>
}

/**
 * The study chat's file functions (docs/study-chat.md, How a file is shared): ask to share
 * (request-upload), send the bytes to the signed URL, have them checked (finalize-upload), and
 * open one (get-download-url). URLs come back as paths on the project; the app puts its own
 * Supabase address in front.
 */
interface ChatFilesRemote {
    suspend fun requestUpload(partyId: String, name: String, mime: String, sizeBytes: Long): FileCall<RequestUploadReply>

    /**
     * Sends a file's bytes, from [open], to [uploadPath] as [mime], saying how many have gone
     * ([onProgress]). It stops past [maxBytes], whatever the document said its size was.
     */
    suspend fun upload(uploadPath: String, mime: String, maxBytes: Long, open: () -> InputStream?, onProgress: (Long) -> Unit): FileCall<Long>

    suspend fun finalize(fileId: String): FileCall<FinalizeReply>

    suspend fun downloadUrl(fileId: String): FileCall<DownloadReply>
}

/** Raised when a document has more bytes than a file may. */
class TooLargeException : IOException("too_large")

@Singleton
class HttpChatFilesRemote(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val access: AiAccess,
    private val http: OkHttpClient
) : ChatFilesRemote {
    @Inject
    constructor(access: AiAccess) : this(
        BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_ANON_KEY,
        access,
        OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val base get() = supabaseUrl.trim().trimEnd('/')

    private suspend inline fun <reified B, reified R> callFunction(name: String, body: B): FileCall<R> = withContext(Dispatchers.IO) {
        val token = access.accessToken() ?: return@withContext FileCall.SignedOut
        val request = Request.Builder()
            .url("$base/functions/v1/$name")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $token")
            .post(json.encodeToString(body).toRequestBody(JSON))
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> FileCall.Ok(json.decodeFromString<R>(text))
                    response.code == 401 -> FileCall.SignedOut
                    response.code >= 500 -> FileCall.Offline
                    else -> FileCall.Refused(runCatching { json.decodeFromString<FunctionError>(text).error }.getOrDefault("refused"))
                }
            }
        } catch (e: IOException) {
            FileCall.Offline
        }
    }

    override suspend fun requestUpload(partyId: String, name: String, mime: String, sizeBytes: Long): FileCall<RequestUploadReply> =
        callFunction<RequestUploadBody, RequestUploadReply>("request-upload", RequestUploadBody(partyId, name, mime, sizeBytes))

    override suspend fun finalize(fileId: String): FileCall<FinalizeReply> =
        callFunction<FileIdBody, FinalizeReply>("finalize-upload", FileIdBody(fileId))

    override suspend fun downloadUrl(fileId: String): FileCall<DownloadReply> =
        callFunction<FileIdBody, DownloadReply>("get-download-url", FileIdBody(fileId))

    override suspend fun upload(uploadPath: String, mime: String, maxBytes: Long, open: () -> InputStream?, onProgress: (Long) -> Unit): FileCall<Long> = withContext(Dispatchers.IO) {
        // Only the project's own storage, at the path the server chose: never anywhere else.
        if (!uploadPath.startsWith("/storage/v1/object/upload/sign/study-files/")) return@withContext FileCall.Refused("bad_upload_url")
        var sent = 0L
        val body = object : RequestBody() {
            override fun contentType() = mime.toMediaType()
            override fun writeTo(sink: BufferedSink) {
                val input = open() ?: throw IOException("unreadable")
                input.use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        sent += read
                        if (sent > maxBytes) throw TooLargeException()
                        sink.write(buffer, 0, read)
                        onProgress(sent)
                    }
                }
            }
        }
        val request = Request.Builder()
            .url(base + uploadPath)
            .header("apikey", anonKey)
            .header("x-upsert", "false")
            .put(body)
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val said = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> FileCall.Ok(sent)
                    response.code >= 500 -> FileCall.Offline
                    // Already there: sent before, its answer lost on the way back. Finalize checks what's stored.
                    said.contains("Duplicate", ignoreCase = true) || said.contains("already exists", ignoreCase = true) -> FileCall.Ok(sent)
                    else -> FileCall.Refused("upload_refused")
                }
            }
        } catch (e: TooLargeException) {
            FileCall.Refused("too_large")
        } catch (e: IOException) {
            if (e.message == "unreadable") FileCall.Refused("unreadable") else FileCall.Offline
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}

@kotlinx.serialization.Serializable
data class FileIdBody(@kotlinx.serialization.SerialName("file_id") val fileId: String)
