package com.pixelquest.app.ui.theme

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.pixelquest.app.domain.model.CrtFilterPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A theme switch recomposes live: content inside PixelQuestTheme sees the new colour scheme
 * without a restart, and the CRT overlay follows the theme.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeRecompositionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun themeSwitch_recomposesColorTokensAccurately() {
        var mode by mutableStateOf(ThemeMode.Pixel)
        var seen: AppColorScheme? = null
        composeTestRule.setContent {
            // Reduce motion skips the 300 ms cross-fade, so each switch lands at once.
            PixelQuestTheme(themeMode = mode, isReduceMotion = true) { seen = PixelTheme.colors }
        }

        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.Pixel, seen!!.themeMode)
        assertTrue(seen!!.isDark)
        assertEquals(PixelBackgroundDark, seen!!.background)

        mode = ThemeMode.Light
        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.Light, seen!!.themeMode)
        assertFalse(seen!!.isDark)
        assertEquals(DefaultLightColorScheme.background, seen!!.background)

        mode = ThemeMode.Comic
        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.Comic, seen!!.themeMode)
        assertFalse(seen!!.isDark) // Comic is a light, paper-backed theme
        assertEquals(DefaultComicColorScheme.background, seen!!.background)
    }

    @Test
    fun themeSwitch_disablesCrtOverlayOnNonPixelThemes() {
        fun crt(mode: ThemeMode, setting: Boolean = true) =
            CrtFilterPolicy.shouldApplyCrt(isCrtSettingEnabled = setting, effectiveThemeMode = mode, isSimpleModeEnabled = false)

        assertTrue(crt(ThemeMode.Pixel))
        assertFalse(crt(ThemeMode.Light))
        assertFalse(crt(ThemeMode.Comic))
        assertFalse(crt(ThemeMode.Pixel, setting = false))
    }
}
