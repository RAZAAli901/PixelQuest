package com.pixelquest.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * S037, S038: release builds talk https only, trust only the system's certificate authorities, and
 * pin the Supabase host to its CAs' keys with an expiry; debug builds may use plain http only to
 * this computer (the local Supabase stack).
 */
class NetworkSecurityConfigTest {

    private fun file(path: String) = listOf(File(path), File("app/$path")).first { it.exists() }
    private fun parse(path: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file(path)).documentElement
    private fun Element.children(tag: String) = getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }

    @Test
    fun release_httpsOnly_systemCAsOnly() {
        val config = parse("src/main/res/xml/network_security_config.xml")
        val base = config.children("base-config").single()
        assertEquals("false", base.getAttribute("cleartextTrafficPermitted"))
        assertEquals(listOf("system"), base.children("certificates").map { it.getAttribute("src") })
        assertTrue("no user CAs anywhere", config.children("certificates").none { it.getAttribute("src") == "user" })
        assertTrue("no cleartext domain", config.children("domain-config").none { it.getAttribute("cleartextTrafficPermitted") == "true" })
        assertTrue("no debug overrides in release", config.children("debug-overrides").isEmpty())
    }

    @Test
    fun theManifest_usesTheConfig_andForbidsCleartext() {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        assertTrue("android:networkSecurityConfig=\"@xml/network_security_config\"" in manifest)
        assertTrue("android:usesCleartextTraffic=\"false\"" in manifest)
    }

    @Test
    fun debug_plainHttpOnlyToThisComputer() {
        val config = parse("src/debug/res/xml/network_security_config.xml")
        assertEquals("false", config.children("base-config").single().getAttribute("cleartextTrafficPermitted"))
        val cleartext = config.children("domain-config").filter { it.getAttribute("cleartextTrafficPermitted") == "true" }
            .flatMap { it.children("domain") }.map { it.textContent.trim() }.toSet()
        assertEquals(setOf("10.0.2.2", "127.0.0.1", "localhost"), cleartext)
    }

    @Test
    fun supabase_isPinned_withBackups_andAnExpiry() {
        for (path in listOf("src/main/res/xml/network_security_config.xml", "src/debug/res/xml/network_security_config.xml")) {
            val config = parse(path)
            val pinned = config.children("domain-config").single { d -> d.children("domain").any { it.textContent.trim() == "supabase.co" } }
            assertEquals(path, "true", pinned.children("domain").single().getAttribute("includeSubdomains"))
            val set = pinned.children("pin-set").single()
            val pins = set.children("pin").map { it.textContent.trim() }
            assertTrue("$path: the root serving today (GTS Root R4)", "mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=" in pins)
            assertTrue("$path: backups for other CAs", pins.size >= 5)
            assertTrue("$path: every pin is a SHA-256 hash", pins.all { it.length == 44 && it.endsWith("=") })
            val expiry = java.time.LocalDate.parse(set.getAttribute("expiration"))
            assertTrue("$path: an expiry, so a forgotten rotation can't lock players out", expiry.isAfter(java.time.LocalDate.of(2026, 10, 22)))
            assertTrue("$path: not too far out", expiry.isBefore(java.time.LocalDate.of(2028, 12, 31)))
            assertEquals("not cleartext", "", pinned.getAttribute("cleartextTrafficPermitted"))
        }
    }
}
