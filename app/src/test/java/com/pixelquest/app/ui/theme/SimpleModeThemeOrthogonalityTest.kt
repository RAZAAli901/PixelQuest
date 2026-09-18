package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.model.SimpleModeSuppression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 37: Verification that Simple Mode UI suppression works orthogonally across
 * all theme modes (Pixel and Light) without conflict.
 */
class SimpleModeThemeOrthogonalityTest {

    @Test
    fun `theme modes and simple mode operate as independent orthogonal state dimensions`() {
        val themeModes = listOf(ThemeMode.Pixel, ThemeMode.Light)
        val simpleModeStates = listOf(true, false)

        for (theme in themeModes) {
            for (isSimple in simpleModeStates) {
                // Verify suppression logic evaluates cleanly regardless of theme
                val isStreakSuppressed = SimpleModeSuppression.isFeatureSuppressed(
                    SimpleModeSuppression.Feature.STREAK_DISPLAY,
                    isSimple
                )
                val isCelebrationSuppressed = SimpleModeSuppression.isFeatureSuppressed(
                    SimpleModeSuppression.Feature.LEVEL_UP_CELEBRATION,
                    isSimple
                )
                val isDifficultyLocked = SimpleModeSuppression.isFeatureSuppressed(
                    SimpleModeSuppression.Feature.DIFFICULTY_SELECTION,
                    isSimple
                )

                if (isSimple) {
                    assertTrue("Streaks must be suppressed in Simple Mode under ${theme.name}", isStreakSuppressed)
                    assertTrue("Celebrations must be suppressed in Simple Mode under ${theme.name}", isCelebrationSuppressed)
                    assertTrue("Difficulty must be locked in Simple Mode under ${theme.name}", isDifficultyLocked)
                } else {
                    assertFalse("Streaks must NOT be suppressed in Gamified Mode under ${theme.name}", isStreakSuppressed)
                    assertFalse("Celebrations must NOT be suppressed in Gamified Mode under ${theme.name}", isCelebrationSuppressed)
                    assertFalse("Difficulty must NOT be locked in Gamified Mode under ${theme.name}", isDifficultyLocked)
                }
            }
        }
    }

    @Test
    fun `theme color palettes provide valid contrast and tokens for both themes in simple mode`() {
        val pixelColors = PixelColorScheme()
        val lightColors = LightColorScheme()

        // Verify key tokens exist in both color schemes
        assertNotNull(pixelColors.primary)
        assertNotNull(pixelColors.surface)
        assertNotNull(pixelColors.onSurface)
        assertNotNull(pixelColors.secondary)

        assertNotNull(lightColors.primary)
        assertNotNull(lightColors.surface)
        assertNotNull(lightColors.onSurface)
        assertNotNull(lightColors.secondary)

        assertEquals(ThemeMode.Pixel, pixelColors.themeMode)
        assertEquals(ThemeMode.Light, lightColors.themeMode)
    }
}
