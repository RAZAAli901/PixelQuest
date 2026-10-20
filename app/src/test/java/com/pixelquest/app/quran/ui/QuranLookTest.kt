package com.pixelquest.app.quran.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.reader.ReaderTags
import com.pixelquest.app.quran.ui.reader.ReaderUiState
import com.pixelquest.app.quran.ui.reader.SurahReaderScreen
import com.pixelquest.app.ui.theme.AppColorScheme
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Quran's rows framed as each look frames its panels, and its text in its own typefaces in every look. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class QuranLookTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun framesIn(vararg modes: ThemeMode, simple: Boolean = false): Map<ThemeMode, Pair<QuranFrame, AppColorScheme>> {
        var mode by mutableStateOf(modes.first())
        var frame: QuranFrame? = null
        var colors: AppColorScheme? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                CompositionLocalProvider(LocalQuranSimpleMode provides simple) {
                    frame = quranFrame()
                    colors = PixelTheme.colors
                }
            }
        }
        return modes.associateWith { each ->
            mode = each
            composeTestRule.waitForIdle()
            frame!! to colors!!
        }
    }

    @Test
    fun pixel_isChamfered_inThePixelBorder() {
        val (frame, colors) = framesIn(ThemeMode.Pixel).getValue(ThemeMode.Pixel)

        assertTrue(frame.shape is CutCornerShape)
        assertEquals(colors.pixelBorder, frame.border!!.brush.let { (it as androidx.compose.ui.graphics.SolidColor).value })
        assertNull(frame.shadow)
    }

    @Test
    fun comic_isInked_withAFlatShadow() {
        val (frame, _) = framesIn(ThemeMode.Comic).getValue(ThemeMode.Comic)

        assertEquals(ComicShapeTokens.ShapeDefault, frame.shape)
        assertEquals(ComicTokens.SolidBlack, (frame.border!!.brush as androidx.compose.ui.graphics.SolidColor).value)
        assertEquals(ComicShapeTokens.BorderWidthThin, frame.border!!.width)
        assertNotNull(frame.shadow)
    }

    @Test
    fun light_isRounded_withAThinBorder() {
        val (frame, colors) = framesIn(ThemeMode.Light).getValue(ThemeMode.Light)

        assertTrue(frame.shape is RoundedCornerShape)
        assertEquals(1.dp, frame.border!!.width)
        assertEquals(colors.pixelBorder, (frame.border!!.brush as androidx.compose.ui.graphics.SolidColor).value)
        assertNull(frame.shadow)
    }

    @Test
    fun simpleMode_isAPlainBlock_inEveryLook() {
        val frames = framesIn(ThemeMode.Pixel, ThemeMode.Comic, ThemeMode.Light, simple = true)

        for ((mode, pair) in frames) {
            val (frame, colors) = pair
            assertTrue("rounded in $mode", frame.shape is RoundedCornerShape)
            assertNull("no border in $mode", frame.border)
            assertNull("no shadow in $mode", frame.shadow)
            assertEquals(colors.surface, frame.fill)
        }
    }

    @Test
    fun simpleMode_roundsThePixelChoices() {
        var shape: androidx.compose.ui.graphics.Shape? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                CompositionLocalProvider(LocalQuranSimpleMode provides true) { shape = quranChipShape() }
            }
        }
        composeTestRule.waitForIdle()

        assertTrue(shape is RoundedCornerShape)
    }

    @Test
    fun theQuranScreen_inSimpleMode_inEachLook_stillWorks() {
        val surah = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 1, null)
        var mode by mutableStateOf(ThemeMode.Pixel)
        val opened = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                CompositionLocalProvider(LocalQuranSimpleMode provides true) {
                    QuranHomeScreen(
                        onNavigateBack = {},
                        list = com.pixelquest.app.quran.ui.list.SurahListUiState(
                            surahs = listOf(surah), loaded = true, ayahCounts = mapOf(2 to 286),
                            continueReading = com.pixelquest.app.quran.ui.list.ContinueReading(surah, 255)
                        ),
                        onOpenAyah = { s, a -> opened += s to a }
                    )
                }
            }
        }
        for (each in listOf(ThemeMode.Pixel, ThemeMode.Comic, ThemeMode.Light)) {
            mode = each
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(com.pixelquest.app.quran.ui.list.CONTINUE_READING).performClick()
        }

        assertEquals(List(3) { 2 to 255 }, opened)
    }

    private fun SemanticsNodeInteraction.fontFamily(): FontFamily? {
        val layouts = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        return layouts.first().layoutInput.style.fontFamily
    }

    @Test
    fun comic_setsTheQuranInItsOwnTypefaces_notComics() {
        // Stand-ins, not the Quran's text.
        val surah = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 1, "b")
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                SurahReaderScreen(state = ReaderUiState(surah = surah, ayahs = listOf(Ayah(8, 2, 1, "a", "t", "en.test"))), onNavigateBack = {})
            }
        }

        assertEquals(AmiriQuran, composeTestRule.onNodeWithTag(ReaderTags.arabic(1), useUnmergedTree = true).fontFamily())
        assertEquals(FontFamily.SansSerif, composeTestRule.onNodeWithTag(ReaderTags.translation(1), useUnmergedTree = true).fontFamily())
    }
}
