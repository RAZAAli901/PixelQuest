package com.pixelquest.app.tune.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.pixelquest.app.tune.model.TuneItem
import com.pixelquest.app.util.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TuneCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient
) {
    private val tunesDir: File by lazy {
        File(context.filesDir, "tunes").apply { if (!exists()) mkdirs() }
    }

    companion object {
        private const val TAG = "TuneCacheManager"
        private const val MAX_FILE_BYTES = 5 * 1024 * 1024L // 5 MB
        private val SAFE_TUNE_ID = Regex("^[a-z0-9-]+$")
    }

    fun getCachedFile(tuneId: String): File? {
        if (!SAFE_TUNE_ID.matches(tuneId)) return null
        val candidate = File(tunesDir, "$tuneId.ogg")
        if (candidate.exists() && candidate.isFile && candidate.length() > 0) return candidate
        val mp3Candidate = File(tunesDir, "$tuneId.mp3")
        if (mp3Candidate.exists() && mp3Candidate.isFile && mp3Candidate.length() > 0) return mp3Candidate
        val wavCandidate = File(tunesDir, "$tuneId.wav")
        if (wavCandidate.exists() && wavCandidate.isFile && wavCandidate.length() > 0) return wavCandidate
        return null
    }

    fun isCached(tuneId: String): Boolean {
        return getCachedFile(tuneId) != null
    }

    /** Cheap cache presence is enough for the library; alarms use this stronger checksum check. */
    fun isCacheValid(tuneId: String): Boolean {
        val file = getCachedFile(tuneId) ?: return false
        return matchesStoredChecksum(tuneId, file)
    }

    fun getUriForTune(tuneId: String): Uri? {
        val file = getCachedFile(tuneId) ?: return null
        if (!isCacheValid(tuneId)) return null
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.tune.provider",
                file
            )
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to get URI for tune $tuneId", e)
            null
        }
    }

    suspend fun downloadAndCache(tune: TuneItem, downloadUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (!SAFE_TUNE_ID.matches(tune.id) || tune.bytes !in 1..MAX_FILE_BYTES) return@withContext false
        val extension = when (tune.mimeType) {
            "audio/mpeg" -> "mp3"
            "audio/wav" -> "wav"
            else -> "ogg"
        }
        val targetFile = File(tunesDir, "${tune.id}.$extension")
        val tempFile = File(tunesDir, "${tune.id}.tmp_${System.currentTimeMillis()}")
        var committed = false

        try {
            val response = httpClient.get(downloadUrl)
            if (!response.status.isSuccess()) return@withContext false
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            FileOutputStream(tempFile).use { output ->
                val channel = response.bodyAsChannel()
                while (true) {
                    val read = channel.readAvailable(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_FILE_BYTES) throw IllegalStateException("Tune exceeds cache limit")
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
            if (total != tune.bytes) return@withContext false
            val hexHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hexHash.equals(tune.sha256, ignoreCase = true)) {
                AppLog.e(TAG, "SHA-256 checksum mismatch for tune ${tune.id}")
                return@withContext false
            }

            val backup = File(tunesDir, "${tune.id}.backup")
            val metadataFile = metadataFile(tune.id)
            if (backup.exists()) backup.delete()
            val hadPrevious = targetFile.exists()
            if (hadPrevious && !targetFile.renameTo(backup)) return@withContext false
            if (tempFile.renameTo(targetFile)) {
                try {
                    metadataFile.writeText("$total\n$hexHash")
                } catch (e: Exception) {
                    targetFile.delete()
                    if (hadPrevious) backup.renameTo(targetFile)
                    AppLog.e(TAG, "Failed to store checksum for tune ${tune.id}", e)
                    return@withContext false
                }
                backup.delete()
                committed = true
                true
            } else {
                if (hadPrevious) backup.renameTo(targetFile)
                false
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to download tune ${tune.id}", e)
            false
        } finally {
            if (!committed && tempFile.exists()) tempFile.delete()
        }
    }

    fun removeTune(tuneId: String): Boolean {
        val file = getCachedFile(tuneId) ?: return false
        val deleted = file.delete()
        if (deleted) metadataFile(tuneId).delete()
        return deleted
    }

    fun getTotalCacheSizeBytes(): Long {
        return tunesDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun metadataFile(tuneId: String): File = File(tunesDir, "$tuneId.meta")

    /** Notification channels must never use a partially written or modified local file. */
    private fun matchesStoredChecksum(tuneId: String, file: File): Boolean {
        return try {
            val lines = metadataFile(tuneId).readLines()
            val expectedBytes = lines.getOrNull(0)?.toLongOrNull() ?: return false
            val expectedHash = lines.getOrNull(1)?.lowercase() ?: return false
            if (file.length() != expectedBytes || !expectedHash.matches(Regex("^[a-f0-9]{64}$"))) return false
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } == expectedHash
        } catch (e: Exception) {
            AppLog.e(TAG, "Could not verify cached tune $tuneId", e)
            false
        }
    }
}
