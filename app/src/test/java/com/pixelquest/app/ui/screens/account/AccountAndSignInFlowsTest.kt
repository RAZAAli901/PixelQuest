package com.pixelquest.app.ui.screens.account

import android.app.Application
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.auth.FakeGoogleAuthManager
import com.pixelquest.app.auth.FakeUserProfileRepositoryForAuth
import com.pixelquest.app.auth.GoogleAuthResult
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.validation.InputRules
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Account and sign-in flow and limit test suite (V038):
 * - Complete user journey: Sign in with Google, see account & sync status, edit settings, sign out
 * - Cancelled sign-in flow
 * - Offline sign-in handling with error display and retry
 * - Expired session with server unreachable (keeps offline state without abrupt logout)
 * - Remote account deletion (clears local cloud id and drops back to signed-out UI)
 * - Sign out everywhere failure when offline
 * - Public leaderboard display name validation & character boundaries
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AccountAndSignInFlowsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeGoogleAuthManager: FakeGoogleAuthManager
    private lateinit var fakeUserProfileRepo: FakeUserProfileRepositoryForAuth
    private lateinit var fakeCloudRepo: FakeCloudProfileRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepository()
        fakeGoogleAuthManager = FakeGoogleAuthManager(context)
        fakeUserProfileRepo = FakeUserProfileRepositoryForAuth()
        fakeCloudRepo = FakeCloudProfileRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun flow_googleSignIn_useApp_andSignOut_returnsToSignedOut() {
        val authVm = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo, fakeUserProfileRepo)
        val accountVm = AccountViewModel(fakeUserProfileRepo, fakeCloudRepo, fakeAuthRepo)

        composeTestRule.setContent {
            val authState by authVm.uiState.collectAsState()
            val accountState by accountVm.uiState.collectAsState()
            val emailState by authVm.emailState.collectAsState()

            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = authState,
                    accountState = accountState,
                    onSignInWithGoogle = { authVm.signInWithGoogle(context) },
                    emailState = emailState,
                    onSignOut = {
                        accountVm.cancelActiveSync()
                        authVm.signOut()
                    },
                    onDisplayNameChange = { accountVm.onDisplayNameChanged(it) },
                    cloudAvailable = true,
                    googleAvailable = true,
                )
            }
        }

        // 1. Initially signed out
        composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").assertIsDisplayed()
        assertNull(fakeUserProfileRepo.storedSupabaseUserId)

        // Configure successful Google & Supabase exchange
        val testUser = AuthUser("uid_flow_123", "journey@pixelquest.com", "JourneyHero")
        fakeGoogleAuthManager.signInResult = GoogleAuthResult.Success(
            idToken = "token_google_valid",
            email = testUser.email!!,
            displayName = testUser.displayName!!,
        )
        fakeAuthRepo.exchangeResult = SupabaseResult.Success(testUser)

        // 2. Perform sign in
        composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").performScrollTo().performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // 3. User is signed in and sees account info
        composeTestRule.onNodeWithText("journey@pixelquest.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIGN OUT").performScrollTo().assertIsDisplayed()
        assertEquals("uid_flow_123", fakeUserProfileRepo.storedSupabaseUserId)

        // 4. Perform sign out
        composeTestRule.onNodeWithText("SIGN OUT").performScrollTo().performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // 5. Back to signed out state
        composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").performScrollTo().assertIsDisplayed()
        assertNull(fakeUserProfileRepo.storedSupabaseUserId)
    }

    @Test
    fun limits_cancelledSignIn_cleanReturnToSignedOutWithoutCrash() {
        val authVm = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo, fakeUserProfileRepo)
        val accountVm = AccountViewModel(fakeUserProfileRepo, fakeCloudRepo, fakeAuthRepo)

        composeTestRule.setContent {
            val authState by authVm.uiState.collectAsState()
            val accountState by accountVm.uiState.collectAsState()

            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = authState,
                    accountState = accountState,
                    onSignInWithGoogle = { authVm.signInWithGoogle(context) },
                    cloudAvailable = true,
                    googleAvailable = true,
                )
            }
        }

        fakeGoogleAuthManager.signInResult = GoogleAuthResult.Cancelled()

        composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").performScrollTo().performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // Remains signed out cleanly, no error banner displayed
        assertTrue(authVm.uiState.value is AuthUiState.SignedOut)
        composeTestRule.onNodeWithText("Couldn't sign in").assertDoesNotExist()
        composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").assertIsDisplayed()
    }

    @Test
    fun limits_offlineSignIn_showsErrorBannerWithRetry() {
        val authVm = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo, fakeUserProfileRepo)
        val accountVm = AccountViewModel(fakeUserProfileRepo, fakeCloudRepo, fakeAuthRepo)

        composeTestRule.setContent {
            val authState by authVm.uiState.collectAsState()
            val accountState by accountVm.uiState.collectAsState()

            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = authState,
                    accountState = accountState,
                    onSignInWithGoogle = { authVm.signInWithGoogle(context) },
                    cloudAvailable = true,
                    googleAvailable = true,
                )
            }
        }

        fakeGoogleAuthManager.signInResult = GoogleAuthResult.Failure(
            IOException("Network unreachable"),
            "No internet connection detected. Please connect to the internet to sign in.",
        )

        composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").performScrollTo().performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // Displays error card and retry action
        assertTrue(authVm.uiState.value is AuthUiState.Error)
        composeTestRule.onNodeWithText("Couldn't sign in").assertIsDisplayed()
        composeTestRule.onNodeWithText("No internet connection detected.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("TRY AGAIN").assertIsDisplayed()
    }

    @Test
    fun limits_expiredSession_keepsSessionOfflineWithoutDroppingUser() {
        val expiredUser = AuthUser("uid_offline_exp", "offline@pixelquest.test", "OfflineKnight")
        val authState = AuthUiState.SignedIn(user = expiredUser, serverUnreachable = true)
        val accountState = AccountUiState()

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = authState,
                    accountState = accountState,
                    cloudAvailable = true,
                    googleAvailable = true,
                )
            }
        }

        // Must display server unreachable warning banner
        composeTestRule.onNodeWithText(
            "Can't reach the server right now. You're still signed in: sync, the leaderboard and the AI Coach come back with the connection.",
        ).assertIsDisplayed()

        // User profile and sign out must still be present
        composeTestRule.onNodeWithText("offline@pixelquest.test").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIGN OUT").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun limits_accountDeletedElsewhere_clearsLocalCloudIdAndLogsOut() = runTest {
        val activeUser = AuthUser("uid_purged", "purged@pixelquest.test", "GhostHero")
        fakeAuthRepo.initialUser = activeUser

        val authVm = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo, fakeUserProfileRepo)
        val accountVm = AccountViewModel(fakeUserProfileRepo, fakeCloudRepo, fakeAuthRepo)
        advanceUntilIdle()

        // Initially signed in
        assertTrue(authVm.uiState.value is AuthUiState.SignedIn)
        assertEquals("uid_purged", fakeUserProfileRepo.storedSupabaseUserId)

        // 1. Remote session invalidated/revoked: currentUser emits null -> AuthViewModel transitions to SignedOut
        fakeAuthRepo.userFlow.value = null
        advanceUntilIdle()
        assertTrue(authVm.uiState.value is AuthUiState.SignedOut)

        // 2. Account deletion confirmation: deletes remote cloud profile, clears local cloud data, and signs out
        accountVm.confirmDeleteCloudAccount(authVm)
        advanceUntilIdle()

        assertTrue(fakeCloudRepo.deleteCalled)
        assertNull(fakeUserProfileRepo.storedSupabaseUserId)
        assertTrue(authVm.uiState.value is AuthUiState.SignedOut)
    }

    @Test
    fun limits_signOutEverywhere_offlineShowsWarning() = runTest {
        val signedInUser = AuthUser("uid_multi", "multi@pixelquest.test", "MultiDeviceHero")
        fakeAuthRepo.initialUser = signedInUser

        val authVm = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo, fakeUserProfileRepo)
        advanceUntilIdle()

        val accountVm = AccountViewModel(fakeUserProfileRepo, fakeCloudRepo, fakeAuthRepo)

        composeTestRule.setContent {
            val authState by authVm.uiState.collectAsState()
            val accountState by accountVm.uiState.collectAsState()
            val signOutFailed by authVm.signOutEverywhereFailed.collectAsState()

            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = authState,
                    accountState = accountState,
                    onSignOutEverywhere = { authVm.signOut(everywhere = true) },
                    signOutEverywhereFailed = signOutFailed,
                    cloudAvailable = true,
                    googleAvailable = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Sign out on every device").performScrollTo().performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // Offline failure indicator is displayed
        composeTestRule.onNodeWithTag(AccountTags.SIGN_OUT_EVERYWHERE_FAILED).performScrollTo().assertIsDisplayed()
        // Local session is preserved
        assertTrue(authVm.uiState.value is AuthUiState.SignedIn)
    }

    @Test
    fun limits_leaderboardDisplayName_validatesCharacterAndLengthBounds() {
        // 1. Blank name
        assertEquals("Display name cannot be blank.", AccountViewModel.validateDisplayName(""))
        assertEquals("Display name cannot be blank.", AccountViewModel.validateDisplayName("   "))

        // 2. Too short (< 3 characters)
        assertEquals("Name must be at least 3 characters.", AccountViewModel.validateDisplayName("a"))
        assertEquals("Name must be at least 3 characters.", AccountViewModel.validateDisplayName("ab"))

        // 3. Exact boundary (3 characters)
        assertNull(AccountViewModel.validateDisplayName("abc"))

        // 4. Exact max boundary (20 characters)
        val maxAllowed = "a".repeat(InputRules.Field.DISPLAY_NAME.max)
        assertNull(AccountViewModel.validateDisplayName(maxAllowed))

        // 5. Exceeding max boundary (21 characters)
        val tooLong = "a".repeat(InputRules.Field.DISPLAY_NAME.max + 1)
        assertEquals("Name must not exceed 20 characters.", AccountViewModel.validateDisplayName(tooLong))

        // 6. Invalid characters (spaces inside, symbols, emoji)
        assertEquals("Only alphanumeric characters and underscores allowed.", AccountViewModel.validateDisplayName("Hero Player"))
        assertEquals("Only alphanumeric characters and underscores allowed.", AccountViewModel.validateDisplayName("Hero@123"))
        assertEquals("Only alphanumeric characters and underscores allowed.", AccountViewModel.validateDisplayName("Hero#Quest"))
        assertEquals("Only alphanumeric characters and underscores allowed.", AccountViewModel.validateDisplayName("Hero\uD83D\uDD25"))

        // 7. Valid alphanumeric and underscores
        assertNull(AccountViewModel.validateDisplayName("Pixel_Hero_99"))
        assertNull(AccountViewModel.validateDisplayName("Knight42"))
    }

    private class FakeCloudProfileRepository : CloudProfileRepository {
        var optInCalled: Boolean = false
        var lastDisplayName: String? = null
        var deleteCalled: Boolean = false

        override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
            optInCalled = optIn
            lastDisplayName = displayName
            return SupabaseResult.Success(Unit)
        }

        override suspend fun syncProfileToCloud(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        override suspend fun deleteCloudProfile(): SupabaseResult<Unit> {
            deleteCalled = true
            return SupabaseResult.Success(Unit)
        }
    }
}
