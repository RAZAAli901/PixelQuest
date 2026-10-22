package com.pixelquest.app.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Proof, from the code itself, that the Islamic hub, the Quran, Darood Party, Study parties and the
 * party core never write XP, levels or streaks (docs/islamic-hub.md, docs/study-party.md): none of
 * their code reaches the game's profile, streak, level or quest-log data, on the phone or on the
 * server. Every path, not only those a test happens to run. The Namaz Tracker is apart: its own plan
 * gives XP for prayers, and its tests cover that.
 */
class NoGameDataFromIslamicCodeTest {

    private fun repo(path: String) = listOf(File("../$path"), File(path)).first { it.exists() }

    /** The packages that must leave the game alone. */
    private val packages = listOf("islamic", "quran", "darood", "study", "party")

    private fun sources(): List<File> = packages.flatMap { pkg ->
        repo("app/src/main/java/com/pixelquest/app/$pkg").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /** What reaches the game's data: its DAOs, entities, repositories and tables. */
    private val gameData = listOf(
        "UserProfileDao", "userProfileDao()", "UserProfileEntity", "UserProfileRepository",
        "StreakDao", "streakDao()", "StreakEntity", "StreakRepository",
        "LevelHistoryDao", "levelHistoryDao()", "LevelHistoryEntity", "LevelHistoryRepository",
        "TaskCompletionLogDao", "taskCompletionLogDao()", "TaskCompletionLogEntity", "TaskCompletionRepository",
        "user_profile", "level_history", "task_completion_logs", "FROM streaks", "INTO streaks", "UPDATE streaks",
        "LevelingUseCase", "XpCalculator",
    )

    @Test
    fun theirCodeOnThePhone_neverReachesTheGamesData() {
        val files = sources()
        assertTrue("the scan found the code (${files.size} files)", files.size > 100)
        val reaches = files.flatMap { file ->
            val text = file.readText()
            gameData.filter { it in text }.map { "${file.name}: $it" }
        }
        assertEquals(emptyList<String>(), reaches)
    }

    @Test
    fun theirMigrations_neverWriteTheProfilesOnTheServer() {
        // The parties' and study parties' migrations: 20261019000000 (usernames) on.
        val migrations = repo("supabase/migrations").listFiles()!!.filter { it.name >= "20261019000000" && it.name.endsWith(".sql") }
        assertTrue(migrations.size >= 45)
        val writes = Regex("""(?i)(update\s+(public\.)?profiles|insert\s+into\s+(public\.)?profiles|alter\s+table\s+(public\.)?profiles|delete\s+from\s+(public\.)?profiles)""")
        // S029 and S057 rewrote delete_user_account: deleting an account deletes its
        // profile, as it always has. That's the account's own deletion, not Islamic code.
        val accountDeletions = setOf(
            "20261019000114_definer_search_paths.sql",
            "20261019000118_account_deletion_complete.sql",
        )
        val found = migrations.filter { it.name !in accountDeletions }
            .mapNotNull { file -> writes.find(file.readText())?.let { "${file.name}: ${it.value}" } }
        val deletion = Regex("""(?i)(update\s+(public\.)?profiles|insert\s+into\s+(public\.)?profiles|alter\s+table\s+(public\.)?profiles)""")
        for (file in accountDeletions) {
            assertEquals("the rewrite only deletes", null, deletion.find(repo("supabase/migrations/$file").readText())?.value)
        }
        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun studyPoints_liveOnlyInTheStudyTables() {
        // The points view reads only the study party's own tables.
        // The SQL itself: comments and the grants (FROM public, FROM authenticated) aside.
        val view = repo("supabase/migrations/20261019000042_study_week_scores.sql").readLines()
            .filterNot { it.trimStart().startsWith("--") || it.trimStart().startsWith("REVOKE") || it.trimStart().startsWith("GRANT") }
            .joinToString(" ")
        val tables = Regex("""(?i)(from|join)\s+(public\.)?(\w+)""").findAll(view).map { it.groupValues[3] }.toSet()
        assertTrue("reads $tables", tables.all { it.startsWith("study_") || it == "config" || it == "finished" || it == "studied" })
    }
}
