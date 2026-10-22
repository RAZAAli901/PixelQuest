package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A debug build can point at a Supabase stack on the developer's computer over plain http
 * (docs/LOCAL_SUPABASE.md); nothing else may use http, and release builds never do.
 */
class LocalStackUrlTest {

    @Test
    fun debugBuilds_acceptOnlyTheLocalStackOverHttp() {
        for (url in listOf("http://10.0.2.2:54321", "http://127.0.0.1:54321/", "http://localhost:54321", "http://10.0.2.2")) {
            assertTrue(url, CloudAvailability.isRealProjectUrl(url, allowLocalHttp = true))
        }
        for (url in listOf("http://example.com", "http://10.0.2.2.evil.example", "http://192.168.1.5:54321", "http://localhost.evil:54321")) {
            assertFalse(url, CloudAvailability.isRealProjectUrl(url, allowLocalHttp = true))
        }
        assertTrue(CloudAvailability.isRealProjectUrl("https://abcd.supabase.co", allowLocalHttp = true))
        assertFalse(CloudAvailability.isRealProjectUrl("https://placeholder-project.supabase.co", allowLocalHttp = true))
    }

    @Test
    fun releaseBuilds_neverAcceptHttp() {
        assertFalse(CloudAvailability.isRealProjectUrl("http://10.0.2.2:54321", allowLocalHttp = false))
        assertFalse(CloudAvailability.isRealProjectUrl("http://127.0.0.1:54321", allowLocalHttp = false))
        assertTrue(CloudAvailability.isRealProjectUrl("https://abcd.supabase.co", allowLocalHttp = false))
    }

    @Test
    fun cleartext_isAllowedOnlyInTheDebugSourceSet_andOnlyForTheLocalStack() {
        val app = File("src").takeIf { it.isDirectory } ?: File("app/src")
        // Since S037 every build has a network security config (the certificate pins), and the
        // manifest says no to cleartext in so many words.
        val mainManifest = File(app, "main/AndroidManifest.xml").readText()
        assertTrue(mainManifest.contains("""android:usesCleartextTraffic="false""""))
        assertTrue(mainManifest.contains("networkSecurityConfig"))
        val main = File(app, "main/res/xml/network_security_config.xml").readText()
        assertTrue(main.contains("""<base-config cleartextTrafficPermitted="false">"""))
        assertFalse("release builds allow no cleartext anywhere", main.contains("""cleartextTrafficPermitted="true""""))

        val config = File(app, "debug/res/xml/network_security_config.xml").readText()
        assertTrue(config.contains("""<base-config cleartextTrafficPermitted="false" />"""))
        val cleartext = Regex("""<domain-config cleartextTrafficPermitted="true">([\s\S]*?)</domain-config>""").findAll(config).toList()
        assertEquals(1, cleartext.size)
        val domains = Regex("<domain[^>]*>([^<]+)</domain>").findAll(cleartext.single().groupValues[1]).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("10.0.2.2", "127.0.0.1", "localhost"), domains)
    }
}
