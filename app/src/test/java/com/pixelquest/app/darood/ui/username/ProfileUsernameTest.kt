package com.pixelquest.app.darood.ui.username

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.ui.screens.ProfileContent
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Profile shows the Darood Party username under the hero's name; tapping it opens the username screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ProfileUsernameTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val profile = UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 450)

    @Test
    fun theUsername_isShown_andOpensTheUsernameScreen() {
        var opened = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                ProfileContent(state = ProfileUiState(profile = profile, daroodUsername = "aria_k"), onNavigateToUsername = { opened++ })
            }
        }

        composeTestRule.onNodeWithTag(DaroodTestTags.PROFILE_USERNAME).assertIsDisplayed()
        assertEquals("@aria_k", composeTestRule.onNodeWithTag(DaroodTestTags.PROFILE_USERNAME).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString())
        composeTestRule.onNodeWithTag(DaroodTestTags.PROFILE_USERNAME).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun withoutAUsername_thereIsNoLine() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                ProfileContent(state = ProfileUiState(profile = profile))
            }
        }

        assertTrue(composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(DaroodTestTags.PROFILE_USERNAME)).fetchSemanticsNodes().isEmpty())
    }
}
