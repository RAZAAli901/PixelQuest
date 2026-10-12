package com.pixelquest.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * What Android's own backup may copy: the player's progress and settings, never the sign-in session.
 * With android:allowBackup and no rules, Android backed up everything the app stores, the supabase-kt
 * session tokens included, and restoring it on another phone signed that phone in.
 */
class AndroidBackupRulesTest {

    private fun file(path: String) = listOf(File(path), File("app/$path")).first { it.exists() }

    private val progressAndSettings = setOf(
        "database:pixelquest.db", "database:pixelquest.db-wal", "database:pixelquest.db-shm",
        "sharedpref:pixelquest_settings.xml", "sharedpref:pixelquest_level_up.xml", "sharedpref:pixelquest_cloud_account.xml"
    )

    private fun includes(root: Element): Set<String> {
        val nodes = root.getElementsByTagName("include")
        return (0 until nodes.length).map { nodes.item(it) as Element }.map { "${it.getAttribute("domain")}:${it.getAttribute("path")}" }.toSet()
    }

    private fun parse(path: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file(path)).documentElement

    @Test
    fun androidUpTo11_backsUpOnlyProgressAndSettings() {
        val rules = parse("src/main/res/xml/backup_rules.xml")
        assertEquals(progressAndSettings, includes(rules))
        assertEquals("nothing excluded by name: what isn't included stays out", 0, rules.getElementsByTagName("exclude").length)
    }

    @Test
    fun android12AndUp_cloudBackupAndPhoneTransfer_theSame() {
        val rules = parse("src/main/res/xml/data_extraction_rules.xml")
        for (section in listOf("cloud-backup", "device-transfer")) {
            assertEquals(section, progressAndSettings, includes(rules.getElementsByTagName(section).item(0) as Element))
        }
    }

    @Test
    fun theDatabaseIncluded_isTheAppsDatabase() {
        val module = file("src/main/java/com/pixelquest/app/di/DatabaseModule.kt").readText()
        assertTrue("DatabaseModule opens pixelquest.db", "\"pixelquest.db\"" in module)
    }

    @Test
    fun theManifest_usesTheRules() {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        assertTrue("android:fullBackupContent=\"@xml/backup_rules\"" in manifest)
        assertTrue("android:dataExtractionRules=\"@xml/data_extraction_rules\"" in manifest)
    }
}
