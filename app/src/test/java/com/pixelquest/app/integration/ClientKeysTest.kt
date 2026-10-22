package com.pixelquest.app.integration

import com.pixelquest.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * S007: the app carries only Supabase's publishable key. A service-role (or secret) key in the app
 * would bypass every row-level policy, so neither the build nor the app's code may reach one.
 */
class ClientKeysTest {

    private val repo = File("").absoluteFile.let { if (File(it, "app").isDirectory) it else it.parentFile }
    private val main = File(repo, "app/src/main")
    private val script = File(repo, "app/build.gradle.kts").readText()

    /** A Supabase JWT key's role claim, or null for any other kind of key. */
    private fun jwtRole(key: String): String? {
        val parts = key.split(".")
        if (parts.size != 3 || !parts[0].startsWith("eyJ")) return null
        val json = String(Base64.getUrlDecoder().decode(parts[1].padEnd((parts[1].length + 3) / 4 * 4, '=')))
        return Regex("\"role\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
    }

    @Test
    fun theBuiltKey_isThePublishableOne() {
        val key = BuildConfig.SUPABASE_ANON_KEY
        assertFalse("never a secret key", key.startsWith("sb_secret_"))
        jwtRole(key)?.let { role -> assertEquals("a JWT key must be the anon role", "anon", role) }
    }

    @Test
    fun theBuild_definesNoServiceKey() {
        val fields = Regex("""buildConfigField\("String",\s*"([A-Z_]+)"""").findAll(script).map { it.groupValues[1] }.toSet()
        assertTrue("the publishable key is there", "SUPABASE_ANON_KEY" in fields)
        val forbidden = fields.filter { Regex("SERVICE|SECRET|ADMIN|PRIVATE").containsMatchIn(it) }
        assertEquals("no build field for a server key", emptyList<String>(), forbidden)
        assertFalse(script.contains("SERVICE_ROLE"))
        assertFalse(script.contains("FCM_SERVICE_ACCOUNT"))
    }

    @Test
    fun theAppsCode_neverNamesAServerKey() {
        val sources = main.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml", "json", "properties") }.toList()
        assertTrue(sources.size > 100)
        val found = sources.flatMap { file ->
            val text = file.readText()
            listOf("service_role", "SERVICE_ROLE", "sb_secret_", "FCM_SERVICE_ACCOUNT", "-----BEGIN PRIVATE KEY")
                .filter { it in text }.map { "${file.name}: $it" }
        }
        assertEquals(emptyList<String>(), found)
    }
}
