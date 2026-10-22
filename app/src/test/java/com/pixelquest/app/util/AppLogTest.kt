package com.pixelquest.app.util

import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.File

/** S055: a release build writes nothing sensitive to Logcat. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppLogTest {

    @After
    fun tearDown() {
        AppLog.release = false
        ShadowLog.clear()
    }

    @Test
    fun release_writesNoDebugLines_andNoExceptionText() {
        AppLog.release = true
        ShadowLog.clear()
        AppLog.d("T", "seeded with Therapy session")
        AppLog.e("T", "Could not sync", IllegalStateException("token eyJhbGciOi for player@example.com"))
        val logs = ShadowLog.getLogsForTag("T")
        assertEquals(1, logs.size)
        assertEquals("Could not sync (IllegalStateException)", logs.single().msg)
        assertNull("no stack trace", logs.single().throwable)
        assertFalse(logs.single().msg.contains("example.com"))
    }

    @Test
    fun debug_writesEverything() {
        AppLog.release = false
        ShadowLog.clear()
        AppLog.d("T", "hello")
        AppLog.w("T", "careful", RuntimeException("why"))
        val logs = ShadowLog.getLogsForTag("T")
        assertEquals(2, logs.size)
        assertEquals("why", logs[1].throwable?.message)
    }

    @Test
    fun theAppLogsOnlyThroughAppLog_andTheLibrariesAreQuiet() {
        val app = listOf(File("src"), File("app/src")).first { it.isDirectory }.absoluteFile
        val direct = app.resolve("main/java").walkTopDown().filter { it.isFile && it.extension == "kt" && it.name != "AppLog.kt" }
            .filter { Regex("""android\.util\.Log\b|^import android\.util\.Log$|\bprintStackTrace\(\)""", RegexOption.MULTILINE).containsMatchIn(it.readText()) }
            .map { it.name }.toList()
        assertEquals(emptyList<String>(), direct)
        val rules = app.parentFile.resolve("proguard-rules.pro").readText()
        assertTrue(rules.contains("-assumenosideeffects class android.util.Log"))
        val supabase = app.resolve("main/java/com/pixelquest/app/data/remote/SupabaseClient.kt").readText()
        assertTrue(supabase.contains("LogLevel.NONE"))
    }
}
