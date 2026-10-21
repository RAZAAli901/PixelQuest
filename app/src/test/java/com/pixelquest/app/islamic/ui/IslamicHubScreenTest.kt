package com.pixelquest.app.islamic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Islamic hub's screen (the Deen tab's first screen): its title, and a card for each item that opens the item's route. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class IslamicHubScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val items = listOf(
        IslamicItem("namaz", R.string.namaz_title, R.string.namaz_title, IslamicIcon("🕌"), "islamic/namaz"),
        IslamicItem("darood", R.string.darood_title, R.string.darood_title, IslamicIcon("📿"), "islamic/darood"),
        IslamicItem("quran", R.string.quran_title, R.string.quran_description, IslamicIcon("📖"), "islamic/quran")
    )

    private val opened = mutableListOf<String>()

    private fun show(mode: ThemeMode = ThemeMode.Pixel, comingSoon: Boolean = false) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                IslamicHubContent(state = IslamicHubUiState(items = items, loaded = true), onOpen = { opened += it }, showComingSoon = comingSoon)
            }
        }
    }

    @Test
    fun theHub_hasItsTitle_andACardPerItem() {
        show()

        composeTestRule.onNodeWithTag(IslamicTestTags.HUB).assertIsDisplayed()
        composeTestRule.onNodeWithText("DEEN").assertIsDisplayed()
        items.forEach { composeTestRule.onNodeWithTag(IslamicTestTags.card(it.id)).assertIsDisplayed() }
    }

    @Test
    fun aCard_opensItsItemsRoute() {
        show()

        composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).performClick()
        composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).performClick()

        assertEquals(listOf("islamic/darood", "islamic/quran"), opened)
    }

    @Test
    fun namazAcrossTheTop_thenTwoTilesARow_sharingItsHeight() {
        show()

        val namaz = composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).getBoundsInRoot()
        val darood = composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).getBoundsInRoot()
        val quran = composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).getBoundsInRoot()

        assertTrue("the Namaz Tracker comes first", darood.top >= namaz.bottom)
        assertEquals("Darood and the Quran share a row", darood.top, quran.top)
        assertEquals(darood.bottom, quran.bottom)
        assertEquals("Darood on the left, under Namaz", namaz.left, darood.left)
    }

    @Test
    fun theComingSoonCard_followsTheItems_onlyWhenAskedFor_andOpensNothing() {
        show(comingSoon = true)

        val darood = composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).getBoundsInRoot()
        val soon = composeTestRule.onNodeWithTag(IslamicTestTags.COMING_SOON).assertIsDisplayed().getBoundsInRoot()
        assertEquals("it starts the row after the last pair, on the left", darood.left, soon.left)
        composeTestRule.onNodeWithTag(IslamicTestTags.COMING_SOON).performClick()
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun withoutTheFlag_noComingSoonCard() {
        show()

        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(IslamicTestTags.COMING_SOON)).fetchSemanticsNodes().size)
    }
}
