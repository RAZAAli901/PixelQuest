package com.pixelquest.app.ui.navigation

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI v2's bottom bar (docs/ui-v2.md): Home, Quests, the trophy, Deen and Hero, in both skins. The
 * trophy sits in the middle and opens the Leaderboard; the Deen tab goes with the Islamic section,
 * and carries Darood's dot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PqBottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun resetSkin() {
        PqSkinState.skin = PqSkin.Comic
    }

    private var selected: PqTab? = null
    private var trophyTaps = 0

    private fun show(
        skin: PqSkin = PqSkin.Comic,
        current: PqTab? = PqTab.Home,
        showDeen: Boolean = true,
        deenDot: Boolean = false,
        isSimpleMode: Boolean = false,
    ) {
        PqSkinState.skin = skin
        composeTestRule.setContent {
            PqBottomBar(
                current = current,
                onSelect = { selected = it },
                onCenterClick = { trophyTaps++ },
                showDeen = showDeen,
                deenDot = deenDot,
                isSimpleMode = isSimpleMode,
            )
        }
    }

    private fun tab(tab: PqTab) = composeTestRule.onNodeWithTag(PqNavTags.tab(tab))

    @Test
    fun theTabs_inOrder_withTheirNames() {
        show()
        listOf(PqTab.Home to "Home", PqTab.Quests to "Quests", PqTab.Deen to "Deen", PqTab.Hero to "Hero").forEach { (t, name) ->
            tab(t).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(name)))
        }
        val lefts = PqTab.entries.map { tab(it).getBoundsInRoot().left.value }
        assertEquals("left to right", lefts.sorted(), lefts)
    }

    private fun trophyOpensTheLeaderboard(skin: PqSkin) {
        show(skin)
        composeTestRule.onNodeWithContentDescription("Leaderboard").performClick()
        assertEquals(1, trophyTaps)
    }

    @Test fun comic_theTrophyOpensTheLeaderboard() = trophyOpensTheLeaderboard(PqSkin.Comic)

    @Test fun pixel_theTrophyOpensTheLeaderboard() = trophyOpensTheLeaderboard(PqSkin.Pixel)

    @Test
    fun aTab_selectsIt_andTheCurrentOneIsMarked() {
        show(current = PqTab.Quests)
        tab(PqTab.Quests).assertIsSelected()
        tab(PqTab.Home).assertIsNotSelected()
        composeTestRule.onNodeWithTag(PqNavTags.TROPHY).assertIsNotSelected()

        tab(PqTab.Deen).performClick()
        assertEquals(PqTab.Deen, selected)
    }

    @Test
    fun onTheLeaderboard_theTrophyIsTheOneSelected() {
        show(current = null)
        composeTestRule.onNodeWithTag(PqNavTags.TROPHY).assertIsSelected()
        PqTab.entries.forEach { tab(it).assertIsNotSelected() }
    }

    private fun assertTrophyCentred() {
        val bar = composeTestRule.onRoot().getBoundsInRoot()
        val trophy = composeTestRule.onNodeWithTag(PqNavTags.TROPHY).getBoundsInRoot()
        val middle = (bar.left + bar.right) / 2
        val trophyMiddle = (trophy.left + trophy.right) / 2
        assertTrue("trophy at $trophyMiddle, bar's middle $middle", (trophyMiddle - middle).value in -1f..1f)
    }

    @Test fun theTrophy_isInTheMiddle() {
        show()
        assertTrophyCentred()
    }

    @Test
    fun withTheIslamicSectionHidden_noDeenTab_andTheTrophyStaysInTheMiddle() {
        show(showDeen = false)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag(PqNavTags.tab(PqTab.Deen))).fetchSemanticsNodes().size)
        tab(PqTab.Hero).assertIsDisplayed()
        assertTrophyCentred()
    }

    @Test
    fun aWaitingDaroodInvitation_dotsTheDeenTab_andSaysSo() {
        show(deenDot = true)
        composeTestRule.onNodeWithTag(PqNavTags.dot(PqTab.Deen), useUnmergedTree = true).assertIsDisplayed()
        tab(PqTab.Deen).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Deen, something waits for you here")))
    }

    @Test
    fun noInvitation_noDot() {
        show()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag(PqNavTags.dot(PqTab.Deen)), useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun simpleMode_callsQuestsTasks() {
        show(isSimpleMode = true)
        tab(PqTab.Quests).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Tasks")))
    }

    @Test
    fun theTabs_areFullHeightTargets() {
        show()
        PqTab.entries.forEach { t ->
            val b = tab(t).getBoundsInRoot()
            assertTrue("$t is ${b.bottom - b.top} tall", b.bottom - b.top >= 48.dp)
        }
    }
}
