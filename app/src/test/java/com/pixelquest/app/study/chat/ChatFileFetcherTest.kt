package com.pixelquest.app.study.chat

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.study.chat.data.HttpChatFileFetcher
import com.pixelquest.app.study.chat.data.OpenedFile
import com.pixelquest.app.study.chat.data.remote.ChatFilesRemote
import com.pixelquest.app.study.chat.data.remote.DownloadReply
import com.pixelquest.app.study.chat.data.remote.FileCall
import com.pixelquest.app.study.chat.data.remote.FinalizeReply
import com.pixelquest.app.study.chat.data.remote.RequestUploadReply
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Opening a shared file: saved in the app's own folder, only as the server checked it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChatFileFetcherTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val bytes = "%PDF-1.7 the real paper".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private var link: FileCall<DownloadReply> = FileCall.Ok(DownloadReply("/storage/v1/object/sign/study-files/p1/x?token=t&download=Past", 60, "Past paper.pdf", "application/pdf", bytes.size.toLong()))
    private var served = bytes
    private var status = 200
    private val asked = mutableListOf<String>()

    private val remote = object : ChatFilesRemote {
        override suspend fun requestUpload(partyId: String, name: String, mime: String, sizeBytes: Long): FileCall<RequestUploadReply> = FileCall.Offline
        override suspend fun upload(uploadPath: String, mime: String, maxBytes: Long, open: () -> InputStream?, onProgress: (Long) -> Unit): FileCall<Long> = FileCall.Offline
        override suspend fun finalize(fileId: String): FileCall<FinalizeReply> = FileCall.Offline
        override suspend fun downloadUrl(fileId: String) = link
    }
    private val http = OkHttpClient.Builder().addInterceptor { chain ->
        asked += chain.request().url.toString()
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("x")
            .body(served.toResponseBody("application/pdf".toMediaType())).build()
    }.build()
    private val fetcher = HttpChatFileFetcher(context, remote, "https://project.example", http)
    private val file = ChatFile("f1", "p1", "bram", "Past paper.pdf", "application/pdf", bytes.size.toLong(), hash, ChatFileStatus.READY, null, false, java.time.Instant.EPOCH, null)

    @Test
    fun aFile_isSavedInItsOwnFolder_asTheServerCheckedIt() = runBlocking {
        val saved = fetcher.fetch(file) as OpenedFile.Saved
        assertEquals(File(context.cacheDir, "study-files/f1/Past paper.pdf").canonicalPath, saved.file.canonicalPath)
        assertTrue(saved.file.readBytes().contentEquals(bytes))
        assertEquals(listOf("https://project.example/storage/v1/object/sign/study-files/p1/x?token=t&download=Past"), asked)
    }

    @Test
    fun bytesThatArentTheCheckedOnes_arentKept() = runBlocking {
        served = "%PDF-1.7 something else!".toByteArray()
        assertEquals(OpenedFile.Corrupted, fetcher.fetch(file))
        assertTrue(File(context.cacheDir, "study-files/f1").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun notTheirsToOpen_orNotTheProjectsStorage_getsNothing() = runBlocking {
        link = FileCall.Refused("not_allowed")
        assertEquals(OpenedFile.NotAllowed, fetcher.fetch(file))
        link = FileCall.Ok(DownloadReply("/somewhere/else", 60, "x.pdf", "application/pdf", 1))
        assertEquals(OpenedFile.NotAllowed, fetcher.fetch(file))
        assertTrue("nothing fetched from elsewhere", asked.isEmpty())
        link = FileCall.Ok(DownloadReply("/storage/v1/object/sign/study-files/p1/x?token=expired", 60, "x.pdf", "application/pdf", 1))
        status = 400
        assertEquals("an expired link", OpenedFile.NotAllowed, fetcher.fetch(file))
    }

    @Test
    fun aHostileName_staysInItsFolder() = runBlocking {
        link = FileCall.Ok(DownloadReply("/storage/v1/object/sign/study-files/p1/x?token=t", 60, "../../../shared_prefs/evil.xml", "application/pdf", bytes.size.toLong()))
        val saved = fetcher.fetch(file) as OpenedFile.Saved
        assertEquals(File(context.cacheDir, "study-files/f1").canonicalPath, saved.file.canonicalFile.parentFile!!.canonicalPath)
        assertEquals("evil.xml", saved.file.name)
    }
}
