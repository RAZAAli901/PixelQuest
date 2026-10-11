package com.pixelquest.app.auth

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.ui.screens.account.AccountContent
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * A signed-in player whose session can't be refreshed (offline, or the server failing) is still
 * signed in. supabase-kt keeps the session saved but no longer current, and Account used to say
 * "signed out" (and the leaderboard "sign in") until the connection came back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class OfflineSessionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val savedUser = Json { ignoreUnknownKeys = true }
        .decodeFromString<UserInfo>("""{"id":"acct-1","aud":"authenticated","email":"hero@pixelquest.test"}""")

    /** A saved session whose access token has expired, with no network to refresh it. */
    private fun offlineClient() = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
        httpEngine = MockEngine { throw java.io.IOException("offline") }
        install(Auth) {
            sessionManager = MemorySessionManager(
                UserSession(
                    accessToken = "expired",
                    refreshToken = "r",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = savedUser,
                    expiresAt = Clock.System.now() - 1.hours
                )
            )
            codeVerifierCache = MemoryCodeVerifierCache()
            autoLoadFromStorage = true
            alwaysAutoRefresh = true
            enableLifecycleCallbacks = false
        }
        install(Postgrest)
    }

    @Test
    fun aFailedRefresh_keepsTheAccountSignedIn_andSaysTheServerIsUnreachable() = runBlocking {
        val supabase = offlineClient()
        val repository = AuthRepositoryImpl(supabase.auth, supabase.postgrest)

        withTimeout(10.seconds) { supabase.auth.sessionStatus.first { it is SessionStatus.RefreshFailure } }

        val expected = AuthUser("acct-1", "hero@pixelquest.test", null)
        assertEquals(expected, repository.currentUser.first())
        assertTrue(repository.serverUnreachable.first())
        assertEquals("the screen's first check agrees", expected, repository.getInitialUser())
    }

    @Test
    fun theViewModel_saysSignedIn_withTheServerUnreachable() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val user = AuthUser("acct-1", "hero@pixelquest.test", null)
        val auth = object : AuthRepository by FakeAuthRepository() {
            override val currentUser = MutableStateFlow<AuthUser?>(user)
            override val serverUnreachable = MutableStateFlow(true)
            override suspend fun getInitialUser() = user
        }
        val viewModel = AuthViewModel(FakeGoogleAuthManager(io.mockk.mockk(relaxed = true)), auth, com.pixelquest.app.testing.FakeUserProfileRepository())
        advanceUntilIdle()

        assertEquals(AuthUiState.SignedIn(user, serverUnreachable = true), viewModel.uiState.value)

        auth.serverUnreachable.value = false // back online
        advanceUntilIdle()
        assertEquals(AuthUiState.SignedIn(user, serverUnreachable = false), viewModel.uiState.value)
    }

    @Test
    fun account_saysTheServerIsUnreachable_notSignedOut() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = AuthUiState.SignedIn(AuthUser("acct-1", "hero@pixelquest.test", null), serverUnreachable = true),
                    accountState = AccountUiState(),
                    cloudAvailable = true
                )
            }
        }
        composeTestRule.onNodeWithText("Can't reach the server right now", substring = true).assertExists()
        composeTestRule.onNodeWithText("hero@pixelquest.test").assertExists()
    }
}
