package com.pixelquest.app.darood.ui.username

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.data.local.entity.UserProfileEntity
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

/** Hero's card shows the Darood Party username under the hero's name; tapping it opens the username screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ProfileUsernameTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val profile = UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 450)

    @androidx.compose.runtime.Composable
    private fun hero(state: ProfileUiState, onUsername: () -> Unit = {}) = com.pixelquest.app.ui.screens.HeroContent(
        state = com.pixelquest.app.ui.screens.heroUiOf(state, com.pixelquest.app.ui.screens.stats.StatsUiState(), java.time.LocalDate.of(2026, 10, 21)),
        segment = com.pixelquest.app.ui.screens.HeroSegment.Profile,
        onSegmentChange = {}, onSettings = {}, onChangeAvatar = {}, onDifficulty = {}, onQuestHistory = {}, onLevelHistory = {}, onLeaderboard = {},
        onUsername = onUsername,
    )

    @Test
    fun theUsername_isShown_andOpensTheUsernameScreen() {
        var opened = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                hero(ProfileUiState(profile = profile, daroodUsername = "aria_k"), onUsername = { opened++ })
            }
        }

        composeTestRule.onNodeWithTag(DaroodTestTags.PROFILE_USERNAME).assertIsDisplayed()
        assertEquals("@aria_k", composeTestRule.onNodeWithTag(DaroodTestTags.PROFILE_USERNAME).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString())
        assertEquals("Darood Party username, @aria_k", composeTestRule.onNodeWithTag(DaroodTestTags.PROFILE_USERNAME).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString())
        composeTestRule.onNodeWithTag(DaroodTestTags.PROFILE_USERNAME).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun withoutAUsername_thereIsNoLine() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                hero(ProfileUiState(profile = profile))
            }
        }

        assertTrue(composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(DaroodTestTags.PROFILE_USERNAME)).fetchSemanticsNodes().isEmpty())
    }
}
