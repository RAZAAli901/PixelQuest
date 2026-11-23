package com.pixelquest.app.tune

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.tune.data.TuneCacheManager
import com.pixelquest.app.tune.model.TuneItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
class TuneCacheManagerTest {

    private lateinit var context: Context
    private val testAudioBytes = "SAMPLE_AUDIO_OGG_CONTENT".toByteArray()
    private lateinit var testSha256: String

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        androidx.startup.AppInitializer.getInstance(context)
            .initializeComponent(com.russhwolf.settings.SettingsInitializer::class.java)

        val md = MessageDigest.getInstance("SHA-256")
        testSha256 = md.digest(testAudioBytes).joinToString("") { "%02x".format(it) }

        // Clear tunes dir
        val tunesDir = File(context.filesDir, "tunes")
        if (tunesDir.exists()) tunesDir.deleteRecursively()
    }

    @Test
    fun downloadAndCache_verifiesChecksumAndSavesFile() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = testAudioBytes,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "audio/ogg")
            )
        }
        val client = HttpClient(mockEngine)
        val manager = TuneCacheManager(context, client)

        val tune = TuneItem(
            id = "test-tune-1",
            name = "Test Tune",
            description = "Test Description",
            categories = listOf("quest"),
            objectKey = "tunes/test.ogg",
            previewObjectKey = "tunes/previews/test.ogg",
            mimeType = "audio/ogg",
            bytes = testAudioBytes.size.toLong(),
            durationMillis = 5000,
            sha256 = testSha256
        )

        val success = manager.downloadAndCache(tune, "https://test.url/tune.ogg")
        assertTrue(success)
        assertTrue(manager.isCached("test-tune-1"))
        assertNotNull(manager.getCachedFile("test-tune-1"))
        assertEquals(testAudioBytes.size.toLong(), manager.getCachedFile("test-tune-1")?.length())
        assertTrue(manager.isCacheValid("test-tune-1"))

        manager.getCachedFile("test-tune-1")!!.writeBytes("corrupt".toByteArray())
        assertFalse(manager.isCacheValid("test-tune-1"))
    }

    @Test
    fun downloadAndCache_rejectsMismatchedChecksum() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = testAudioBytes,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "audio/ogg")
            )
        }
        val client = HttpClient(mockEngine)
        val manager = TuneCacheManager(context, client)

        val tune = TuneItem(
            id = "corrupt-tune",
            name = "Corrupt Tune",
            description = "Test Description",
            categories = listOf("quest"),
            objectKey = "tunes/test.ogg",
            previewObjectKey = "tunes/previews/test.ogg",
            mimeType = "audio/ogg",
            bytes = testAudioBytes.size.toLong(),
            durationMillis = 5000,
            sha256 = "0000000000000000000000000000000000000000000000000000000000000000"
        )

        val success = manager.downloadAndCache(tune, "https://test.url/tune.ogg")
        assertFalse(success)
        assertFalse(manager.isCached("corrupt-tune"))
        assertNull(manager.getCachedFile("corrupt-tune"))
        assertTrue(File(context.filesDir, "tunes").listFiles().orEmpty().none { it.name.contains(".tmp_") })
    }

    @Test
    fun cachedFile_rejectsUnsafeId() {
        val manager = TuneCacheManager(context, HttpClient(MockEngine { error("No network request expected") }))

        assertNull(manager.getCachedFile("../outside"))
        assertFalse(manager.isCached("../outside"))
    }
}
