package com.pixelquest.app.islamic.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
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

/** The hub with TalkBack and large text: one button per card, read as a sentence, in reading order. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class IslamicHubAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val items = listOf(
        IslamicItem(
            "namaz", R.string.namaz_title, R.string.islamic_namaz_description, IslamicIcon("🕌"), "islamic/namaz",
            badgeCount = 2, badgeLabel = R.plurals.islamic_namaz_badge
        ),
        IslamicItem(
            "darood", R.string.darood_title, R.string.islamic_darood_description, IslamicIcon("📿"), "islamic/darood",
            badgeCount = 1, badgeLabel = R.plurals.islamic_darood_badge
        ),
        IslamicItem("quran", R.string.quran_title, R.string.quran_description, IslamicIcon("📖"), "islamic/quran")
    )

    private fun show(fontScale: Float = 1f, simpleMode: Boolean = false) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PixelQuestTheme(themeMode = ThemeMode.Comic) {
                    IslamicHubContent(IslamicHubUiState(items, isSimpleMode = simpleMode, loaded = true), onOpen = {})
                }
            }
        }
    }

    private fun spoken(id: String): String =
        composeTestRule.onNodeWithTag(IslamicTestTags.card(id)).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)!!.joinToString(" ")

    @Test
    fun eachCard_isOneButton_readAsASentence_withItsBadgeInWords() {
        show()

        assertEquals("Namaz Tracker. Prayer times, reminders and your week. 2 prayers to mark", spoken("namaz"))
        assertEquals("Darood Party. Count Darood together with friends. 1 invitation waiting", spoken("darood"))
        assertEquals("Quran. Read the Quran in Arabic and English", spoken("quran"))

        val darood = composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).fetchSemanticsNode()
        assertEquals(Role.Button, darood.config.getOrNull(SemanticsProperties.Role))
        assertEquals("open", darood.config.getOrNull(SemanticsActions.OnClick)?.label)
    }

    @Test
    fun noEmojiNames_andNoBareNumbers() {
        show()

        for (text in listOf("🕌", "📿", "📖", "2", "1")) {
            assertEquals(text, 0, composeTestRule.onAllNodes(hasText(text)).fetchSemanticsNodes().size)
        }
    }

    @Test
    fun theCards_comeInReadingOrder() {
        show()

        val order = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .mapNotNull { node -> items.firstOrNull { node.config.getOrNull(SemanticsProperties.TestTag) == IslamicTestTags.card(it.id) }?.id }
        assertEquals(listOf("namaz", "darood", "quran"), order)
    }

    @Test
    fun atLargeText_oneCardARow() {
        show(fontScale = 2f)

        val namaz = composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).getBoundsInRoot()
        val darood = composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).getBoundsInRoot()
        assertEquals(namaz.left, darood.left)
        assertTrue(darood.top >= namaz.bottom)
    }

    @Test
    fun theCards_areBigEnoughToTap() {
        show(simpleMode = true)

        for (item in items) {
            val bounds = composeTestRule.onNodeWithTag(IslamicTestTags.card(item.id)).getBoundsInRoot()
            val height = bounds.bottom - bounds.top
            assertTrue("${item.id}: $height", height.value >= 48f)
        }
    }

    @Test
    fun inSimpleMode_tooTheCardIsOneSentence() {
        show(simpleMode = true)

        assertEquals("Namaz Tracker. Prayer times, reminders and your week. 2 prayers to mark", spoken("namaz"))
        assertEquals(0, composeTestRule.onAllNodes(hasText("2")).fetchSemanticsNodes().size)
    }
}
