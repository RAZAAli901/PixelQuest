package com.pixelquest.app.auth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.data.repository.PendingLeaderboardChoice
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Signing out sends a leave from the leaderboard that is still waiting to be sent (made offline, say),
 * while the session can still send it. The player used to stay public on the board until they
 * signed in again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SignOutSendsLeaveTest {

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun signOutWith(pendingOptIn: Boolean?): List<String> {
        val events = mutableListOf<String>()
        val pending = PendingLeaderboardChoice(context.getSharedPreferences("signout-$pendingOptIn", Context.MODE_PRIVATE))
        if (pendingOptIn != null) pending.choose("acct", optIn = pendingOptIn, displayName = "Titan_Slayer")
        val user = AuthUser("acct", "hero@pixelquest.test", null)
        val auth = object : AuthRepository by FakeAuthRepository() {
            override val currentUser = MutableStateFlow<AuthUser?>(user)
            override suspend fun getInitialUser() = user
            override suspend fun signOut(): SupabaseResult<Unit> {
                events += "sign out"
                return SupabaseResult.Success(Unit)
            }
        }
        val cloud = object : CloudProfileRepository {
            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = SupabaseResult.Success(Unit)
            override suspend fun syncProfileToCloud() = SupabaseResult.Success(Unit)
            override suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> {
                events += "leave"
                return SupabaseResult.Success(Unit)
            }
        }
        val profiles = FakeUserProfileRepository(UserProfileEntity(username = "Hero", avatarId = "avatar_hero", supabaseUserId = "acct"))
        runTest(StandardTestDispatcher()) {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = AuthViewModel(FakeGoogleAuthManager(io.mockk.mockk(relaxed = true)), auth, profiles, null, cloud, pending)
            advanceUntilIdle()
            viewModel.signOut()
            advanceUntilIdle()
        }
        return events
    }

    @Test
    fun aLeaveStillWaiting_isSentBeforeSigningOut() {
        assertEquals(listOf("leave", "sign out"), signOutWith(pendingOptIn = false))
    }

    @Test
    fun nothingWaiting_justSignsOut() {
        assertEquals(listOf("sign out"), signOutWith(pendingOptIn = null))
    }

    @Test
    fun aJoinWaiting_isNotSentAsALeave() {
        assertEquals(listOf("sign out"), signOutWith(pendingOptIn = true))
    }
}
