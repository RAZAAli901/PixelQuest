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

    /**
     * S033: every preferences file the app opens has a decision. It's backed up (above), or it
     * belongs to this device and stays out. A new file fails this test until it's put in one list.
     */
    @Test
    fun everyPreferencesFile_isDecided() {
        val deviceOnly = setOf(
            "pixelquest_ai_usage", "pixelquest_chess", "pixelquest_darood", "pixelquest_darood_goals", "pixelquest_darood_week",
            "pixelquest_encouragement_pack", "pixelquest_leaderboard_choice", "pixelquest_namaz_delivered", "pixelquest_namaz_reminders",
            "pixelquest_permissions", "pixelquest_progress_reset", "pixelquest_quran", "pixelquest_reminders_handled",
            "pixelquest_streak_nudge", "pixelquest_study", "pixelquest_study_chat", "pixelquest_study_reminder_mutes",
            "pixelquest_study_reminders", "pixelquest_tips",
        )
        val backedUp = settings.map { it.removePrefix("sharedpref:").removeSuffix(".xml") }.toSet()
        val sources = file("src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.map { it.readText() }.toList()
        val names = sources.flatMap { text ->
            Regex("""getSharedPreferences\(\s*"([a-z0-9_]+)"""").findAll(text).map { it.groupValues[1] }.toList() +
                Regex("""(?:PREFS_NAME|PREFS|HANDLED_PREFS|FILE_NAME)\s*=\s*"(pixelquest_[a-z0-9_]+)"""").findAll(text).map { it.groupValues[1] }.toList()
        }.toSet()
        assertTrue("found the app's preferences files", names.size >= 15)
        val undecided = names - deviceOnly - backedUp
        assertEquals("a new preferences file needs a backup decision (docs/security/STORAGE.md)", emptySet<String>(), undecided)
        assertTrue("nothing is both", (deviceOnly intersect backedUp).isEmpty())
    }

    /** The sign-in session is supabase-kt's default preferences file: no rule may name it. */
    @Test
    fun theSession_isNeverBackedUp() {
        for (path in listOf("src/main/res/xml/backup_rules.xml", "src/main/res/xml/data_extraction_rules.xml")) {
            val text = file(path).readText()
            assertTrue(path, "_preferences" !in text && "supabase" !in text.substringAfter("-->"))
            assertTrue("no whole-domain include in $path", !Regex("""path="\.?/?"""").containsMatchIn(text))
        }
    }
}
