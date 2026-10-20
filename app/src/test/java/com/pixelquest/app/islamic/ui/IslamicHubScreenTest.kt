package com.pixelquest.app.islamic.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.menu.LocalMenuDrawer
import com.pixelquest.app.ui.menu.rememberMenuDrawerController
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Islamic hub's screen: its top bar, and a card for each item that opens the item's route. */
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

    private fun show(mode: ThemeMode = ThemeMode.Pixel, withMenu: Boolean = true, comingSoon: Boolean = false) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                val menu = if (withMenu) rememberMenuDrawerController() else null
                CompositionLocalProvider(LocalMenuDrawer provides menu) {
                    IslamicHubContent(state = IslamicHubUiState(items = items, loaded = true), onOpen = { opened += it }, showComingSoon = comingSoon)
                }
            }
        }
    }

    @Test
    fun theHub_hasItsTitle_theMenuIcon_andACardPerItem() {
        show()

        composeTestRule.onNodeWithTag(IslamicTestTags.HUB).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open menu").assertIsDisplayed()
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
    fun twoCardsARow_sharingItsHeight() {
        show()

        val namaz = composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).getBoundsInRoot()
        val darood = composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).getBoundsInRoot()
        val quran = composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).getBoundsInRoot()

        assertEquals("Namaz and Darood share the first row", namaz.top, darood.top)
        assertEquals(namaz.bottom, darood.bottom)
        assertEquals("the Quran starts the next row, on the left", namaz.left, quran.left)
    }

    @Test
    fun theComingSoonCard_followsTheItems_onlyWhenAskedFor_andOpensNothing() {
        show(comingSoon = true)

        val quran = composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).getBoundsInRoot()
        val soon = composeTestRule.onNodeWithTag(IslamicTestTags.COMING_SOON).assertIsDisplayed().getBoundsInRoot()
        assertEquals("it takes the place beside the last item", quran.top, soon.top)
        composeTestRule.onNodeWithTag(IslamicTestTags.COMING_SOON).performClick()
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun withoutTheFlag_noComingSoonCard() {
        show()

        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(IslamicTestTags.COMING_SOON)).fetchSemanticsNodes().size)
    }

    @Test
    fun withoutAMenu_noMenuIcon() {
        show(withMenu = false)

        composeTestRule.onNodeWithText("Islamic", ignoreCase = true).assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Open menu")).fetchSemanticsNodes().size)
    }
}
