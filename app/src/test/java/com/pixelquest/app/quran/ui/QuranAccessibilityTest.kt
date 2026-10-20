package com.pixelquest.app.quran.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.list.FAST_SCROLL_MARKS
import com.pixelquest.app.quran.ui.list.FastScrollIndex
import com.pixelquest.app.quran.ui.list.FastScrollTags
import com.pixelquest.app.quran.ui.list.QuranListTags
import com.pixelquest.app.quran.ui.list.QuranTab
import com.pixelquest.app.quran.ui.list.QuranTabs
import com.pixelquest.app.quran.ui.reader.AyahItem
import com.pixelquest.app.quran.ui.reader.DisplayMode
import com.pixelquest.app.quran.ui.reader.DisplayModeSwitch
import com.pixelquest.app.quran.ui.reader.ReaderTags
import com.pixelquest.app.quran.ui.reader.ReaderUiState
import com.pixelquest.app.quran.ui.reader.SurahReaderScreen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** TalkBack hears each ayah's meaning; every control is a 48dp target; large text doesn't break the choices. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class QuranAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Stand-ins, not the Quran's text.
    private val ayah = Ayah(262, 2, 255, "a", "a stand-in for the translation", "en.test")

    @Test
    fun anAyah_isReadAsItsNumberAndTranslation_inEveryMode() {
        var mode by mutableStateOf(DisplayMode.BOTH)
        composeTestRule.setContent {
            PixelQuestTheme { AyahItem(ayah = ayah, mode = mode, typography = QuranTypography.of(), onClick = {}) }
        }

        for (each in DisplayMode.entries) {
            mode = each
            composeTestRule.waitForIdle()
            val spoken = composeTestRule.onNodeWithTag(ReaderTags.ayah(255)).fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            assertEquals("in $each", listOf("Ayah 255. a stand-in for the translation"), spoken)
        }
    }

    @Test
    fun theChoices_andTheFastScrollMarks_areFullSizeTargets() {
        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    DisplayModeSwitch(selected = DisplayMode.BOTH, onSelect = {})
                    QuranTabs(selected = QuranTab.SURAHS, onSelect = {})
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                        FastScrollIndex(listState = rememberLazyListState(), surahs = (1..114).toList())
                    }
                }
            }
        }

        for (mode in DisplayMode.entries) composeTestRule.onNodeWithTag(ReaderTags.mode(mode)).assertHeightIsAtLeast(48.dp)
        for (tab in QuranTab.entries) composeTestRule.onNodeWithTag(QuranListTags.tab(tab)).assertHeightIsAtLeast(48.dp)
        for (mark in FAST_SCROLL_MARKS) {
            composeTestRule.onNodeWithTag(FastScrollTags.mark(mark)).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    // Native graphics: Robolectric's default doesn't measure text.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun withLargeText_theDisplayModes_stack_withNoWordBroken() {
        composeTestRule.setContent {
            PixelQuestTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                    DisplayModeSwitch(selected = DisplayMode.BOTH, onSelect = {})
                }
            }
        }

        for (mode in DisplayMode.entries) {
            val label = composeTestRule.onNode(hasAnyAncestor(hasTestTag(ReaderTags.mode(mode))), useUnmergedTree = true).fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            label.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            assertEquals("$mode on one line", 1, layouts.first().lineCount)
            val layout = layouts.first()
            assertTrue("$mode's whole label fits its width", layout.multiParagraph.intrinsics.maxIntrinsicWidth <= layout.layoutInput.constraints.maxWidth)
        }
    }

    @Test
    fun withLargeText_theReader_stillShowsItsAyahs() {
        // Stand-ins, not the Quran's text.
        val surah = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 1, "b")
        composeTestRule.setContent {
            PixelQuestTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                    SurahReaderScreen(state = ReaderUiState(surah = surah, ayahs = listOf(ayah.copy(number = 1))), onNavigateBack = {})
                }
            }
        }

        composeTestRule.onNodeWithTag(ReaderTags.ayah(1)).assertHeightIsAtLeast(48.dp)
    }
}
