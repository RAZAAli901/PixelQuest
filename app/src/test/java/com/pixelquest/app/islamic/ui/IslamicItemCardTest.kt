package com.pixelquest.app.islamic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One tile on Deen: title, description, and a badge only while something waits. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class IslamicItemCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val darood = IslamicItem("darood", R.string.darood_title, R.string.darood_empty_title, IslamicIcon("📿"), "islamic/darood")

    private var clicks = 0

    private fun show(item: IslamicItem, mode: ThemeMode = ThemeMode.Comic) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) { IslamicItemCard(item = item, onClick = { clicks++ }) }
        }
    }

    private fun badges() = composeTestRule.onAllNodes(hasTestTag(IslamicTestTags.BADGE), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun theCard_hasItsIconTitleAndDescription_andOpensOnATap() {
        show(darood)

        composeTestRule.onNodeWithText("Darood Circle", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("No parties yet", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun nothingWaiting_noBadge() {
        show(darood)

        assertEquals(0, badges())
    }

    @Test
    fun aBadge_appearsWhileSomethingWaits() {
        show(darood.copy(badgeCount = 2))

        assertEquals(1, badges())
    }

    @Test
    fun aBadgesNumber_stopsAt99() {
        assertEquals("2", badgeText(2))
        assertEquals("99", badgeText(99))
        assertEquals("99+", badgeText(250))
    }
}
