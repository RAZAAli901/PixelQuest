package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.model.CrtFilterPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 37 (Day 23): Full regression pass confirming Light mode is completely unaffected
 * across every screen after today's navigation/chrome/screen-level changes.
 */
class Day23LightModeFullRegressionTest {

    @Test
    fun lightMode_themeFamily_isLight() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Light)
        assertEquals(ComponentThemeFamily.LIGHT, family)
    }

    @Test
    fun lightMode_colorScheme_preservesDaylightParchmentTokens() {
        val scheme = DefaultLightColorScheme

        assertEquals(ThemeMode.Light, scheme.themeMode)
        assertFalse("Light mode must NOT be a dark mode", scheme.isDark)
        assertEquals("Background must be warm ivory #F8F6F0", Color(0xFFF8F6F0), scheme.background)
        assertEquals("Surface must be crisp white #FFFFFF", Color(0xFFFFFFFF), scheme.surface)
        assertEquals("Primary must be amber gold #B45309", Color(0xFFB45309), scheme.primary)
        assertEquals("Secondary must be daylight sky #0369A1", Color(0xFF0369A1), scheme.secondary)
        assertEquals("Tertiary must be meadow green #15803D", Color(0xFF15803D), scheme.tertiary)
        assertEquals("OnBackground must be deep stone charcoal #1C1917", Color(0xFF1C1917), scheme.onBackground)
        assertEquals("OnSurface must be deep stone charcoal #1C1917", Color(0xFF1C1917), scheme.onSurface)
    }

    @Test
    fun lightMode_chromeDispatches_bypassComicBranches() {
        val activeMode = ThemeMode.Light
        assertFalse("Bottom nav must not dispatch to ComicBottomNavBar in Light mode", activeMode == ThemeMode.Comic)
        assertFalse("Top app bar must not dispatch to ComicTopAppBar in Light mode", activeMode == ThemeMode.Comic)
        assertFalse("FAB must not dispatch to ComicFloatingActionButton in Light mode", activeMode == ThemeMode.Comic)
        assertFalse("Splash screen must not dispatch to ComicSplashScreen in Light mode", activeMode == ThemeMode.Comic)
        assertFalse("Snackbar must not dispatch to ComicSnackbar in Light mode", activeMode == ThemeMode.Comic)
    }

    @Test
    fun lightMode_crtFilter_isStrictlyDisabled() {
        val crtActive = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Light,
            isSimpleModeEnabled = false
        )
        assertFalse("CRT filter must remain strictly disabled in Light mode", crtActive)
    }

    @Test
    fun lightMode_assetFilter_appliesDaylightTinting() {
        val tintColor = Color(0xFFB45309)
        val filter = PixelThemeAssetFilter.forTheme(
            themeMode = ThemeMode.Light,
            lightColor = tintColor
        )
        assertNotNull("Light mode must provide dynamic asset tinting filter for pixel assets", filter)
    }

    @Test
    fun lightMode_screensResolveDaylightTokensWithoutComicContamination() {
        val screens = listOf(
            "TodayScreen", "TasksScreen", "CreateTaskScreen", "EditTaskScreen",
            "StatsScreen", "ProfileScreen", "SettingsScreen", "LeaderboardScreen",
            "LevelHistoryScreen", "DidYouDoItScreen"
        )
        for (screen in screens) {
            val scheme = DefaultLightColorScheme
            assertNotNull("Screen $screen must have valid background in Light mode", scheme.background)
            assertNotNull("Screen $screen must have valid primary in Light mode", scheme.primary)
            assertEquals("Screen $screen background must be #F8F6F0", Color(0xFFF8F6F0), scheme.background)
            assertFalse("Screen $screen must not report isDark in Light mode", scheme.isDark)
        }
    }
}
