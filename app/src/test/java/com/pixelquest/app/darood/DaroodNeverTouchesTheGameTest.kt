package com.pixelquest.app.darood

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.toDomain
import com.pixelquest.app.darood.sync.PartyChange
import com.pixelquest.app.testing.FakeDaroodLive
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Darood never gives XP, and never touches levels, streaks or the leaderboard
 * (docs/darood-design.md, Kept apart from the game). Proved two ways:
 *  - nothing in Darood's code, or its server migrations, names the game's progress classes or tables;
 *  - a whole party session (creating, inviting, posting offline and online, live updates, paging,
 *    the milestone, finishing) leaves the player's XP, level, streak and leaderboard data exactly
 *    as they were.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodNeverTouchesTheGameTest {

    private fun dir(vararg candidates: String) = candidates.map(::File).first { it.isDirectory }

    private val sources = dir("src/main/java/com/pixelquest/app/darood", "app/src/main/java/com/pixelquest/app/darood")
    private val migrations = dir("../supabase/migrations", "supabase/migrations")

    /** What the game's progress is made of: its classes, DAOs, sync, and the server's profile row. */
    private val forbidden = listOf(
        "UserProfileRepository", "UserProfileDao", "UserProfileEntity", "addXp", "totalXp", "perfectDays",
        "StreakRepository", "StreakDao", "StreakEntity", "StreakEvaluation", "LevelHistory", "LevelUpSignal",
        "LeaderboardRepository", "CloudProfileRepository", "CloudProfileDto", "ProfileSyncWorker", "SyncScheduler",
        "TaskCompletionLog", "\"profiles\""
    )

    /** The forbidden names [code] uses, as whole names: Darood's own DaroodSyncScheduler isn't the game's SyncScheduler. */
    private fun violations(code: String): List<String> {
        val lines = code.lines().filterNot { it.trimStart().startsWith("//") || it.trimStart().startsWith("*") }.joinToString("\n")
        return forbidden.filter { name ->
            if (name.startsWith("\"")) name in lines else Regex("""\b${Regex.escape(name)}\b""").containsMatchIn(lines)
        }
    }

    @Test
    fun theCheck_catchesTheGamesNames_andOnlyThose() {
        assertEquals(listOf("UserProfileRepository"), violations("import com.pixelquest.app.domain.repository.UserProfileRepository"))
        assertEquals(listOf("addXp"), violations("profile.addXp(10)"))
        assertEquals(listOf("\"profiles\""), violations("postgrest[\"profiles\"]"))
        assertEquals(emptyList<String>(), violations("class WorkManagerDaroodSyncScheduler : DaroodSyncScheduler"))
        assertEquals("comments don't count", emptyList<String>(), violations("// never calls addXp"))
    }

    @Test
    fun darood_code_namesNoneOfTheGamesProgress() {
        val files = sources.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("the darood package was found", files.size > 10)
        val found = files.flatMap { file -> violations(file.readText()).map { "${file.name}: $it" } }
        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun darood_migrations_neverTouchTheProfilesOrTheBoard() {
        val darood = migrations.listFiles()!!.filter { it.name >= "20261019000004" && it.name.endsWith(".sql") }
        assertTrue("Darood's migrations were found", darood.size >= 20)
        val found = darood.flatMap { file ->
            // Chess's own party ladder (chess_party_leaderboard) isn't the game's leaderboard.
            val sql = file.readLines().filterNot { it.trimStart().startsWith("--") }.joinToString("\n").lowercase().replace("chess_party_leaderboard", "")
            listOf("public.profiles", "total_xp", "current_streak", "longest_streak", "leaderboard").filter { it in sql }.map { "${file.name}: $it" }
        }
        assertEquals(emptyList<String>(), found)
    }

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun aWholePartySession_leavesXpLevelsStreaksAndTheBoardAlone() = runBlocking {
        val hero = UserProfileEntity(
            username = "Aria", avatarId = "avatar_mage", level = 4, totalXp = 1500, perfectDaysTowardNextLevel = 2,
            leaderboardOptIn = true, leaderboardDisplayName = "Aria_K"
        )
        db.userProfileDao().insertProfile(hero)
        db.streakDao().insertStreak(StreakEntity(currentStreak = 3, longestStreak = 9, lastCompletedDate = LocalDate.of(2026, 10, 18), perfectDaysCount = 12))
        val before = db.userProfileDao().getProfile().first() to db.streakDao().getCurrentStreak().first()

        val server = FakeDaroodServer(me = "me").apply { usernames += mapOf("me" to "aria_k", "u2" to "bram") }
        val live = FakeDaroodLive()
        val repository = DaroodPartyRepositoryImpl(
            server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") }, live = live, numbersDelayMs = 0
        )
        val party = (repository.createParty("Friday circle", 10, null) as com.pixelquest.app.data.remote.SupabaseResult.Success).data
        repository.invite(party, "bram")
        server.members.replaceAll { if (it.userId == "u2") it.copy(status = "active") else it }
        repository.refreshParty(party)
        val following = launch(Dispatchers.Unconfined) { repository.followParty(party).collect { } }
        repository.addEntry(party, 313)
        server.offline = true
        repository.addEntry(party, 33)
        server.offline = false
        repository.sendPending()
        live.send(party, PartyChange.EntryAdded(server.seedEntry(party, "u2", 100).toDomain()))
        repository.setMilestone(party, 1000)
        repeat(3) { repository.addEntry(party, 200) }
        repository.loadOlderEntries(party)
        following.cancel()
        assertEquals("the party finished", true, repository.observeParty(party).first()!!.isFinished)

        assertEquals(before, db.userProfileDao().getProfile().first() to db.streakDao().getCurrentStreak().first())
        assertEquals("no level history written", emptyList<Any>(), db.levelHistoryDao().getAllHistory().first())
        assertEquals("no quest results written", emptyList<Any>(), db.taskCompletionLogDao().getAllLogs().first())
    }
}
