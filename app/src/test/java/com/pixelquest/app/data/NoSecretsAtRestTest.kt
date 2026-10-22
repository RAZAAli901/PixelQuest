package com.pixelquest.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * S035: the database holds the player's progress and their parties' content, never a secret.
 * Tokens are sealed with the Keystore (S034) and file links are used once, in memory. So no Room
 * entity may gain a field that would store a token, password, key or link.
 */
class NoSecretsAtRestTest {

    private val main = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }

    @Test
    fun noEntityStoresATokenPasswordKeyOrLink() {
        val entities = main.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { "@Entity" in it.readText() }.toList()
        assertTrue("found the entity files", entities.size >= 15)
        val field = Regex("""\bval\s+(\w+)\s*:""")
        val risky = Regex("""(?i)(token|password|secret|apikey|api_key|signedurl|downloadurl|accesskey|privatekey)""")
        val found = entities.flatMap { file ->
            field.findAll(file.readText()).map { it.groupValues[1] }.filter { risky.containsMatchIn(it) }.map { "${file.name}: $it" }.toList()
        }
        assertEquals("a secret has no place in the database (docs/security/STORAGE.md)", emptyList<String>(), found)
    }
}
