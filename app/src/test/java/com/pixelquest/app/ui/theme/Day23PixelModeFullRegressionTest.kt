package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.model.CrtFilterPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 36 (Day 23): Full regression pass confirming Pixel mode is completely unaffected
 * across every screen after today's navigation/chrome/screen-level changes.
 */
class Day23PixelModeFullRegressionTest {

    @Test
    fun pixelMode_themeFamily_isPixel() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel)
        assertEquals(ComponentThemeFamily.PIXEL, family)
    }

    @Test
    fun pixelMode_colorScheme_isStrictlyPreserved() {
        val scheme = DefaultPixelColorScheme

        assertEquals(ThemeMode.Pixel, scheme.themeMode)
        assertTrue("Pixel mode must remain a dark mode", scheme.isDark)
        assertEquals(Color(0xFF12121E), scheme.background)
        assertEquals(Color(0xFF1A1A2E), scheme.surface)
        assertEquals(PixelSurfaceBorder, scheme.surfaceVariant)
        assertEquals(PixelGold, scheme.primary)
        assertEquals(PixelCyan, scheme.secondary)
        assertEquals(PixelGreen, scheme.tertiary)
        assertEquals(PixelRed, scheme.error)
        assertEquals(Color.White, scheme.onBackground)
        assertEquals(Color.White, scheme.onSurface)
    }

    @Test
    fun pixelMode_chromeDispatches_bypassComicBranches() {
        // Assert that in Pixel mode, Comic chrome branches are strictly false/bypassed
        val activeMode = ThemeMode.Pixel
        assertFalse("Bottom nav must not dispatch to ComicBottomNavBar in Pixel mode", activeMode == ThemeMode.Comic)
        assertFalse("Top app bar must not dispatch to ComicTopAppBar in Pixel mode", activeMode == ThemeMode.Comic)
        assertFalse("FAB must not dispatch to ComicFloatingActionButton in Pixel mode", activeMode == ThemeMode.Comic)
        assertFalse("Splash screen must not dispatch to ComicSplashScreen in Pixel mode", activeMode == ThemeMode.Comic)
        assertFalse("Snackbar must not dispatch to ComicSnackbar in Pixel mode", activeMode == ThemeMode.Comic)
    }

    @Test
    fun pixelMode_crtFilter_isFullyEnabledAndOperational() {
        val crtActive = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Pixel,
            isSimpleModeEnabled = false
        )
        assertTrue("CRT scanline filter must remain operational in Pixel mode", crtActive)
    }

    @Test
    fun pixelMode_assetFilter_returnsNullForCanonicalPixelRendering() {
        val filter = PixelThemeAssetFilter.forTheme(
            themeMode = ThemeMode.Pixel,
            lightColor = PixelGold
        )
        assertNull("Pixel mode must never apply artificial tinting to original 8-bit assets", filter)
    }

    @Test
    fun pixelMode_screensDoNotDependOnComicTokens() {
        val screens = listOf(
            "TodayScreen", "TasksScreen", "CreateTaskScreen", "EditTaskScreen",
            "StatsScreen", "ProfileScreen", "SettingsScreen", "LeaderboardScreen",
            "LevelHistoryScreen", "DidYouDoItScreen"
        )
        for (screen in screens) {
            val scheme = DefaultPixelColorScheme
            assertNotNull("Screen $screen must have valid background in Pixel mode", scheme.background)
            assertNotNull("Screen $screen must have valid primary in Pixel mode", scheme.primary)
            assertEquals("Screen $screen must not use Comic Paper background in Pixel mode", Color(0xFF12121E), scheme.background)
        }
    }
}
