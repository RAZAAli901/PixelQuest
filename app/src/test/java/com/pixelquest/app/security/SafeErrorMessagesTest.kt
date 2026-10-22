package com.pixelquest.app.security

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * S056: no screen shows a server's or a library's own error text. Errors reach the player as the
 * app's fixed sentences (SafeErrorCopy, AiErrorCopy) or as refusal tokens mapped to strings.
 */
class SafeErrorMessagesTest {

    private val app = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }.resolve("com/pixelquest/app")

    /** Where text is put on screen: the UI and the view models, and sign-in. */
    private fun screens() = app.walkTopDown().filter { it.isFile && it.extension == "kt" }
        .filter { f -> val p = f.invariantSeparatorsPath; "/ui/" in p || "/auth/" in p }
        // The developer-only AI trigger shows raw states on purpose, in debug builds only.
        .filterNot { it.name.startsWith("DebugAi") }

    @Test
    fun noScreenInterpolatesAnErrorsOwnText() {
        val raw = Regex("""\$\{[^}]*\b(e|error|exception|throwable|cause|result|exchangeResult|googleResult)\.(message|localizedMessage)\b[^}]*}|\.localizedMessage\b""")
        val found = screens().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> raw.containsMatchIn(line) && "AppLog" !in line }
                .map { (i, _) -> "${file.name}:${i + 1}" }
        }.toList()
        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun noScreenPassesAnExceptionsMessageStraightOn() {
        val raw = Regex("""Error\(\s*(e|error|exception|throwable)\.(message|localizedMessage)""")
        val found = screens().filter { raw.containsMatchIn(it.readText()) }.map { it.name }.toList()
        assertEquals(emptyList<String>(), found)
    }
}
