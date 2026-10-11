package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.data.repository.PendingLeaderboardChoice
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Joining or leaving the leaderboard belongs to the account. A choice made on this phone that the
 * server hasn't confirmed is sent; otherwise the phone follows the server's row. Before, a phone off
 * the board re-sent "off" on every sync and a phone on it re-sent "on", so with one account on two
 * phones whichever synced last decided.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LeaderboardChoiceSyncTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("choice-test", Context.MODE_PRIVATE)
    private val pending = PendingLeaderboardChoice(prefs)

    /** The account's row on the server, and what the worker asked of it. */
    private inner class Server(var row: CloudProfileDto?) : CloudProfileRepository {
        var leaveReachesTheServer = true
        val calls = mutableListOf<String>()
        override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
            calls += "join:$displayName"
            pending.confirmed("acct", optIn) // as CloudProfileRepositoryImpl does
            return SupabaseResult.Success(Unit)
        }
        override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
            calls += "push"
            return SupabaseResult.Success(Unit)
        }
        override suspend fun fetchCloudProfile(userId: String) = SupabaseResult.Success(row)
        override suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> {
            calls += "leave"
            if (leaveReachesTheServer) pending.confirmed("acct", optIn = false)
            return SupabaseResult.Success(Unit)
        }
        var resetAnswer: SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        override suspend fun sendProgressAfterReset(): SupabaseResult<Unit> {
            calls += "reset"
            return resetAnswer
        }
    }

    private fun row(optIn: Boolean, name: String = "Titan_Slayer") =
        CloudProfileDto(id = "acct", displayName = name, level = 1, totalXp = 0, leaderboardOptIn = optIn, updatedAt = "2000-01-01T00:00:00Z")

    private fun phone(optIn: Boolean, name: String? = null) = FakeUserProfileRepository(
        UserProfileEntity(username = "Hero", avatarId = "avatar_hero", supabaseUserId = "acct", leaderboardOptIn = optIn, leaderboardDisplayName = name)
    )

    private val resets = com.pixelquest.app.data.repository.PendingProgressReset(context.getSharedPreferences("reset-sync-test", Context.MODE_PRIVATE))

    private fun sync(profiles: FakeUserProfileRepository, server: Server): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<ProfileSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    ProfileSyncWorker(appContext, workerParameters, profiles, server, FakeStreakRepository(StreakEntity()), pending, resets)
            })
            .build()
            .doWork()
    }

    @Test
    fun theStore_keepsOneChoicePerAccount_untilTheServerConfirmsIt() {
        pending.choose("acct", optIn = false, displayName = null)

        assertEquals(PendingLeaderboardChoice.Choice(false, null), pending.pendingFor("acct"))
        assertNull("another account's choice isn't this one's", pending.pendingFor("other"))
        pending.confirmed("acct", optIn = true)
        assertFalse("a different answer doesn't confirm it", pending.pendingFor("acct") == null)
        pending.confirmed("acct", optIn = false)
        assertNull(pending.pendingFor("acct"))
    }

    @Test
    fun leftOnThisPhone_theLeaveIsSent_andNothingElse() {
        pending.startTracking()
        pending.choose("acct", optIn = false, displayName = null)
        val server = Server(row(optIn = true))

        assertEquals(ListenableWorker.Result.success(), sync(phone(optIn = false), server))
        assertEquals(listOf("leave"), server.calls)
    }

    @Test
    fun joinedOnThisPhone_theJoinIsSentWithItsName() {
        pending.startTracking()
        pending.choose("acct", optIn = true, displayName = "Titan_Slayer")
        val server = Server(row(optIn = false))

        sync(phone(optIn = true, name = "Titan_Slayer"), server)
        assertEquals(listOf("join:Titan_Slayer"), server.calls)
    }

    @Test
    fun leftOnAnotherPhone_thisPhoneFollows_andSendsNothing() {
        pending.startTracking()
        val profiles = phone(optIn = true, name = "Titan_Slayer")
        val server = Server(row(optIn = false))

        sync(profiles, server)

        assertFalse("used to re-join the board", profiles.profile.value!!.leaderboardOptIn)
        assertEquals(emptyList<String>(), server.calls)
    }

    @Test
    fun joinedOnAnotherPhone_thisPhoneFollows_withTheName_andSyncsItsProgress() {
        pending.startTracking()
        val profiles = phone(optIn = false)
        val server = Server(row(optIn = true, name = "Titan_Slayer"))

        sync(profiles, server)

        assertTrue("used to take the account off the board again", profiles.profile.value!!.leaderboardOptIn)
        assertEquals("Titan_Slayer", profiles.profile.value!!.leaderboardDisplayName)
        assertEquals(listOf("push"), server.calls)
    }

    @Test
    fun renamedOnAnotherPhone_thisPhoneFollows() {
        pending.startTracking()
        val profiles = phone(optIn = true, name = "Old_Name")

        sync(profiles, Server(row(optIn = true, name = "New_Name")))

        assertEquals("New_Name", profiles.profile.value!!.leaderboardDisplayName)
    }

    @Test
    fun aSpectatorWhoNeverJoined_sendsNothing() {
        pending.startTracking()
        val server = Server(row = null)

        sync(phone(optIn = false), server)
        assertEquals(emptyList<String>(), server.calls)
    }

    @Test
    fun afterUpdating_aPhoneThatIsOffTheBoard_staysOff_evenIfTheServerSaysOn() {
        // A version without pending choices: its opt-out may never have reached the server.
        val profiles = phone(optIn = false)
        val server = Server(row(optIn = true))

        sync(profiles, server)

        assertFalse(profiles.profile.value!!.leaderboardOptIn)
        assertEquals("its leaving is sent, not undone", listOf("leave"), server.calls)
    }

    @Test
    fun afterAReset_theFreshNumbersAreSentOnce_pastTheNoLowerNumbersGuard() {
        pending.startTracking()
        resets.mark("acct")
        val profiles = phone(optIn = true, name = "Titan_Slayer")
        val server = Server(row(optIn = true))

        assertEquals(ListenableWorker.Result.success(), sync(profiles, server))
        assertEquals(listOf("reset"), server.calls)
        assertFalse(resets.isPendingFor("acct"))

        sync(profiles, server)
        assertEquals("then ordinary syncs again", listOf("reset", "push"), server.calls)
    }

    @Test
    fun aResetMadeOffline_isSentLater() {
        pending.startTracking()
        resets.mark("acct")
        val server = Server(row(optIn = true)).also { it.resetAnswer = SupabaseResult.NetworkError(java.io.IOException("offline")) }

        assertEquals(ListenableWorker.Result.retry(), sync(phone(optIn = true, name = "Titan_Slayer"), server))
        assertTrue(resets.isPendingFor("acct"))
    }

    @Test
    fun aLeaveThatCouldntBeSent_isTriedAgain() {
        pending.startTracking()
        pending.choose("acct", optIn = false, displayName = null)
        // The session can't refresh: CloudProfileRepositoryImpl returns success without sending.
        val server = Server(row(optIn = true)).also { it.leaveReachesTheServer = false }

        assertEquals("used to report success, and nothing tried again", ListenableWorker.Result.retry(), sync(phone(optIn = false), server))
    }
}

