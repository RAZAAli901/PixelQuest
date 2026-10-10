package com.pixelquest.app.auth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
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
}
