package com.pixelquest.app.quran.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pixelquest.app.ui.theme.AppColorScheme
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The reader's page: the look's own (dark at night in Pixel), or sepia in any look, its text readable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ReaderPaperTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun colorsOn(paper: ReaderPaper, mode: ThemeMode): Pair<AppColorScheme, AppColorScheme> {
        lateinit var look: AppColorScheme
        lateinit var page: AppColorScheme
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                look = PixelTheme.colors
                ReaderPage(paper) { page = PixelTheme.colors }
            }
        }
        composeTestRule.waitForIdle()
        return look to page
    }

    @Test
    fun theThemesPage_isTheLooksOwn() {
        val (look, page) = colorsOn(ReaderPaper.THEME, ThemeMode.Pixel)

        assertEquals(look.background, page.background)
        assertFalse("both skins read on light paper", page.isDark)
    }

    @Test
    fun sepia_isPaperAndBrownInk_inAnyLook_keepingTheLook() {
        var mode by mutableStateOf(ThemeMode.Pixel)
        lateinit var look: AppColorScheme
        lateinit var page: AppColorScheme
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                look = PixelTheme.colors
                ReaderPage(ReaderPaper.SEPIA) { page = PixelTheme.colors }
            }
        }
        for (each in listOf(ThemeMode.Pixel, ThemeMode.Comic)) {
            mode = each
            composeTestRule.waitForIdle()
            assertEquals(SEPIA_PAPER, page.background)
            assertFalse(page.isDark)
            assertEquals("the look stays $each", look.themeMode, page.themeMode)
        }
    }

    @Test
    fun sepiasText_isReadableOnItsPaper() {
        val sepia = SepiaColorScheme(com.pixelquest.app.ui.theme.PqAppColors)
        fun contrast(a: Color, b: Color): Float {
            val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
            return (hi + 0.05f) / (lo + 0.05f)
        }
        for (ink in listOf(sepia.onBackground, sepia.onSurface, sepia.onSurfaceVariant, sepia.primaryText)) {
            assertTrue("4.5:1 on the paper", contrast(ink, sepia.background) >= 4.5f)
            assertTrue("4.5:1 on a panel", contrast(ink, sepia.surface) >= 4.5f)
        }
        // The bookmark ribbon (the secondary colour) is a mark, not text: 3:1.
        assertTrue("the ribbon shows on the paper", contrast(sepia.secondary, sepia.background) >= 3f)
    }
}
