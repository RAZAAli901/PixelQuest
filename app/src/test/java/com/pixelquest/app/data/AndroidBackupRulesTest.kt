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

    private val settings = setOf("sharedpref:pixelquest_settings.xml", "sharedpref:pixelquest_level_up.xml", "sharedpref:pixelquest_cloud_account.xml")
    private val database = setOf("database:pixelquest.db", "database:pixelquest.db-wal", "database:pixelquest.db-shm")

    /** To the cloud (S032): the app's own automatic backups and the settings, never the database. */
    private val cloud = settings + "file:auto-backups/"

    /** Phone to phone: the whole game, the database included. */
    private val transfer = cloud + database

    private fun includes(root: Element): Set<String> {
        val nodes = root.getElementsByTagName("include")
        return (0 until nodes.length).map { nodes.item(it) as Element }.map { "${it.getAttribute("domain")}:${it.getAttribute("path")}" }.toSet()
    }

    private fun parse(path: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file(path)).documentElement

    @Test
    fun androidUpTo11_backsUpTheAutomaticBackupsAndSettings_notTheDatabase() {
        val rules = parse("src/main/res/xml/backup_rules.xml")
        assertEquals(cloud, includes(rules))
        assertEquals("nothing excluded by name: what isn't included stays out", 0, rules.getElementsByTagName("exclude").length)
    }

    @Test
    fun android12AndUp_theCloudGetsNoDatabase_aPhoneTransferGetsIt() {
        val rules = parse("src/main/res/xml/data_extraction_rules.xml")
        assertEquals(cloud, includes(rules.getElementsByTagName("cloud-backup").item(0) as Element))
        assertEquals(transfer, includes(rules.getElementsByTagName("device-transfer").item(0) as Element))
    }

    @Test
    fun theAutomaticBackupsFolder_isTheOneAutoBackupsWrites() {
        val autoBackups = file("src/main/java/com/pixelquest/app/data/backup/AutoBackups.kt").readText()
        assertTrue("AutoBackups writes files/auto-backups", "DIR_NAME = \"auto-backups\"" in autoBackups)
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
