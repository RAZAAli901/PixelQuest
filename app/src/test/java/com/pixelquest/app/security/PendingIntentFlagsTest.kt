package com.pixelquest.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * S046: every PendingIntent is immutable, so the app that fires it (the alarm clock, the
 * notification shade) can't fill in or redirect the intent inside. None needs to be mutable: the
 * notifications have no reply box and no bubble. A mutable one would need FLAG_MUTABLE and a
 * comment saying why, and this test makes that a deliberate change.
 */
class PendingIntentFlagsTest {

    private val main = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }

    private fun sources() = main.walkTopDown().filter { it.isFile && it.extension == "kt" }

    /** The argument list of each `PendingIntent.getX(...)` call in [text]. */
    private fun pendingIntentCalls(text: String): List<String> =
        Regex("""PendingIntent\.get(Activity|Broadcast|Service|ForegroundService|Activities)\(""").findAll(text).map { match ->
            var depth = 1
            var i = match.range.last + 1
            while (depth > 0 && i < text.length) {
                when (text[i]) { '(' -> depth++; ')' -> depth-- }
                i++
            }
            text.substring(match.range.last + 1, i - 1)
        }.toList()

    @Test
    fun everyPendingIntent_isImmutable() {
        val calls = sources().flatMap { file -> pendingIntentCalls(file.readText()).map { file.name to it } }.toList()
        assertTrue("found the app's PendingIntents", calls.size >= 25)
        val mutable = calls.filterNot { (_, args) -> "FLAG_IMMUTABLE" in args }.map { it.first }
        assertEquals(emptyList<String>(), mutable)
        assertEquals(emptyList<String>(), sources().filter { "FLAG_MUTABLE" in it.readText() }.map { it.name }.toList())
    }

    @Test
    fun theIntentsInside_nameTheirComponent() {
        // An implicit intent in a PendingIntent could be caught by another app; ours all name a class.
        val implicit = sources().filter { "PendingIntent.get" in it.readText() }
            .filter { Regex("""Intent\(\s*("|Intent\.ACTION_)""").containsMatchIn(it.readText()) }
            .map { it.name }.toList()
        assertEquals(emptyList<String>(), implicit)
    }
}
