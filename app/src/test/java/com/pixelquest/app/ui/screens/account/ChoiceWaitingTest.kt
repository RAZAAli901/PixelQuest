package com.pixelquest.app.ui.screens.account

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.data.repository.PendingLeaderboardChoice
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/**
 * Account says when joining or leaving hasn't reached the server yet (chosen offline), and stops
 * saying so once the background sync gets it there. It used to show the new status as if it were done.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChoiceWaitingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theViewModel_saysWaiting_untilTheServerHasIt() {
        val pending = PendingLeaderboardChoice(context.getSharedPreferences("waiting-vm", Context.MODE_PRIVATE))
        val profiles = FakeUserProfileRepository(UserProfileEntity(username = "Hero", avatarId = "avatar_hero", supabaseUserId = "acct"))
        val cloud = object : CloudProfileRepository {
            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = SupabaseResult.Success(Unit)
            override suspend fun syncProfileToCloud() = SupabaseResult.Success(Unit)
        }
        val viewModel = AccountViewModel(profiles, cloud, pendingChoice = pending)
        assertEquals(false, viewModel.uiState.value.choiceWaiting)

        pending.choose("acct", optIn = false, displayName = null) // left offline
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(true, viewModel.uiState.value.choiceWaiting)

        pending.confirmed("acct", optIn = false) // the background sync sent it
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(false, viewModel.uiState.value.choiceWaiting)
    }

    @Test
    fun account_showsTheWaitingNote() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = AuthUiState.SignedIn(AuthUser("acct", "hero@pixelquest.test", null)),
                    accountState = AccountUiState(isOptedIn = false, choiceWaiting = true),
                    cloudAvailable = true
                )
            }
        }
        composeTestRule.onNodeWithText("Waiting to reach the server", substring = true).assertExists()
    }
}
