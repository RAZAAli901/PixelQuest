package com.pixelquest.app.auth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A second account signed in on the same phone starts with its own choices. The leaderboard opt-in
 * and public name (and the AI Coach consent) are kept on the device, so account B used to be put on
 * the public leaderboard under account A's name without ever opting in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccountSwitchTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences(CloudAccountLink.PREFS_NAME, Context.MODE_PRIVATE)
    private val profiles = FakeUserProfileRepository(UserProfileEntity(username = "Hero", avatarId = "avatar_hero"))
    private val settings = FakeSettingsRepository()
    private val link = CloudAccountLink(prefs, profiles, settings)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun aliceJoinsTheBoard() {
        link.onSignedIn("alice")
        profiles.updateLeaderboardSettings(optIn = true, displayName = "Alpha")
        settings.setAiInsightsEnabled(true)
    }

    @Test
    fun aDifferentAccount_startsFresh() = runBlocking {
        aliceJoinsTheBoard()

        assertTrue(link.onSignedIn("bob"))

        val profile = profiles.profile.value!!
        assertEquals("bob", profile.supabaseUserId)
        assertFalse("Bob hasn't joined the leaderboard", profile.leaderboardOptIn)
        assertNull("and doesn't get Alice's public name", profile.leaderboardDisplayName)
        assertFalse("nor her AI Coach consent", settings.aiInsightsEnabled.value)
    }

    @Test
    fun theSameAccountAgain_keepsItsChoices() = runBlocking {
        aliceJoinsTheBoard()
        profiles.updateSupabaseUserId(null) // signed out

        assertFalse(link.onSignedIn("alice"))

        val profile = profiles.profile.value!!
        assertTrue(profile.leaderboardOptIn)
        assertEquals("Alpha", profile.leaderboardDisplayName)
        assertTrue(settings.aiInsightsEnabled.value)
    }

    @Test
    fun signingInWithAnEmailCode_goesThroughTheLink() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        aliceJoinsTheBoard()
        val auth = object : AuthRepository by FakeAuthRepository() {
            override suspend fun sendEmailCode(email: String) = SupabaseResult.Success(Unit)
            override suspend fun verifyEmailCode(email: String, code: String) = SupabaseResult.Success(AuthUser("bob", email, null))
        }
        val viewModel = AuthViewModel(FakeGoogleAuthManager(io.mockk.mockk(relaxed = true)), auth, profiles, link)
        advanceUntilIdle()

        viewModel.onEmailChanged("bob@pixelquest.test")
        viewModel.sendEmailCode()
        advanceUntilIdle()
        viewModel.onCodeChanged("123456")
        viewModel.verifyEmailCode()
        advanceUntilIdle()

        assertEquals("bob", profiles.profile.value!!.supabaseUserId)
        assertFalse(profiles.profile.value!!.leaderboardOptIn)
    }

    /** The server's profile rows, as each account reads its own; [linkedWhenRead] is the device's link at that moment. */
    private class Rows(vararg rows: CloudProfileDto) : CloudProfileRepository {
        val byId = rows.associateBy { it.id }.toMutableMap()
        var offline = false
        var linkedWhenRead: String? = "not read"
        var profiles: FakeUserProfileRepository? = null
        override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = SupabaseResult.Success(Unit)
        override suspend fun syncProfileToCloud() = SupabaseResult.Success(Unit)
        override suspend fun fetchCloudProfile(userId: String): SupabaseResult<CloudProfileDto?> {
            linkedWhenRead = profiles?.profile?.value?.supabaseUserId
            return if (offline) SupabaseResult.NetworkError(java.io.IOException("offline")) else SupabaseResult.Success(byId[userId])
        }
    }

    private fun row(id: String, name: String, optIn: Boolean) =
        CloudProfileDto(id = id, displayName = name, leaderboardOptIn = optIn)

    @Test
    fun aReturningAccount_getsItsBoardChoiceBack() = runBlocking {
        val rows = Rows(row("alice", "Alpha", optIn = true)).also { it.profiles = profiles }
        val link = CloudAccountLink(prefs, profiles, settings, rows)
        link.onSignedIn("alice")
        profiles.updateLeaderboardSettings(optIn = true, displayName = "Alpha")
        settings.setAiInsightsEnabled(true)
        link.onSignedIn("bob")
        assertFalse(profiles.profile.value!!.leaderboardOptIn)

        assertTrue("still a different account from the last one", link.onSignedIn("alice"))

        val profile = profiles.profile.value!!
        assertEquals("alice", profile.supabaseUserId)
        assertTrue("Alice is still on the board, so the next sync mustn't take her off", profile.leaderboardOptIn)
        assertEquals("Alpha", profile.leaderboardDisplayName)
        assertFalse("the AI Coach consent isn't on the server: she turns it on again", settings.aiInsightsEnabled.value)
    }

    @Test
    fun aReinstall_keepsThePlayerOnTheBoard() = runBlocking {
        val rows = Rows(row("alice", "Alpha", optIn = true))
        val link = CloudAccountLink(prefs, profiles, settings, rows)

        assertFalse(link.onSignedIn("alice"))

        val profile = profiles.profile.value!!
        assertTrue(profile.leaderboardOptIn)
        assertEquals("Alpha", profile.leaderboardDisplayName)
    }

    @Test
    fun anAccountThatLeftTheBoard_staysOffWithItsName() = runBlocking {
        val link = CloudAccountLink(prefs, profiles, settings, Rows(row("alice", "Alpha", optIn = false)))

        link.onSignedIn("alice")

        assertFalse(profiles.profile.value!!.leaderboardOptIn)
        assertEquals("Alpha", profiles.profile.value!!.leaderboardDisplayName)
    }

    @Test
    fun theRowIsReadBeforeTheAccountIsLinked() = runBlocking {
        aliceJoinsTheBoard()
        val rows = Rows(row("bob", "Bravo", optIn = true)).also { it.profiles = profiles }

        CloudAccountLink(prefs, profiles, settings, rows).onSignedIn("bob")

        assertEquals("a sync while it's read still sees the old link", "alice", rows.linkedWhenRead)
        assertTrue(profiles.profile.value!!.leaderboardOptIn)
        assertEquals("Bravo", profiles.profile.value!!.leaderboardDisplayName)
    }

    @Test
    fun theRowCantBeRead_aDifferentAccountStillStartsOff() = runBlocking {
        aliceJoinsTheBoard()
        val rows = Rows(row("bob", "Bravo", optIn = true)).also { it.offline = true }

        assertTrue(CloudAccountLink(prefs, profiles, settings, rows).onSignedIn("bob"))

        assertFalse(profiles.profile.value!!.leaderboardOptIn)
        assertNull(profiles.profile.value!!.leaderboardDisplayName)
    }

    @Test
    fun theSameAccount_isNotReadAgain() = runBlocking {
        aliceJoinsTheBoard()
        profiles.updateSupabaseUserId(null) // signed out
        val rows = Rows(row("alice", "OldName", optIn = false))

        assertFalse(CloudAccountLink(prefs, profiles, settings, rows).onSignedIn("alice"))

        assertEquals("not read", rows.linkedWhenRead)
        assertTrue("this phone's own choice stands", profiles.profile.value!!.leaderboardOptIn)
        assertEquals("Alpha", profiles.profile.value!!.leaderboardDisplayName)
    }

    @Test
    fun linkedBeforeTheDeviceRememberedAccounts_countsAsTheSame() = runBlocking {
        profiles.updateLeaderboardSettings(optIn = true, displayName = "Alpha")
        profiles.updateSupabaseUserId("alice") // an update from a version without CloudAccountLink
        val rows = Rows(row("alice", "Alpha", optIn = true))

        assertFalse(CloudAccountLink(prefs, profiles, settings, rows).onSignedIn("alice"))

        assertEquals("not read", rows.linkedWhenRead)
        assertTrue(profiles.profile.value!!.leaderboardOptIn)
    }
}
