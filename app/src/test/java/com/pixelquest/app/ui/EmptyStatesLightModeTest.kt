package com.pixelquest.app.ui

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Step 33: Verify empty states (EmptyTasksState, Leaderboard not-signed-in state,
 * LevelHistoryScreen's empty state) render correctly with compliant contrast in light mode.
 */
class EmptyStatesLightModeTest {

    private fun calculateLuminance(color: Color): Double {
        fun linearize(c: Float): Double {
            return if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)
    }

    private fun calculateContrastRatio(c1: Color, c2: Color): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    @Test
    fun testEmptyTasksState_lightModeContrastCompliance() {
        val scheme = DefaultLightColorScheme
        // EmptyTasksState uses primary for title and onSurfaceVariant for message over surface (card fill)
        val titleContrast = calculateContrastRatio(scheme.primary, scheme.surface)
        val messageContrast = calculateContrastRatio(scheme.onSurfaceVariant, scheme.surface)

        assertTrue("EmptyTasksState title contrast on surface ($titleContrast) must be >= 4.5:1", titleContrast >= 4.5)
        assertTrue("EmptyTasksState message contrast on surface ($messageContrast) must be >= 4.5:1", messageContrast >= 4.5)
    }

    @Test
    fun testLeaderboardNotSignedInState_lightModeContrastCompliance() {
        val scheme = DefaultLightColorScheme
        // NotSignedInLeaderboardState uses primary for title and onSurface for message over surface
        val titleContrast = calculateContrastRatio(scheme.primary, scheme.surface)
        val bodyContrast = calculateContrastRatio(scheme.onSurface, scheme.surface)

        assertTrue("Leaderboard not-signed-in title contrast ($titleContrast) must be >= 4.5:1", titleContrast >= 4.5)
        assertTrue("Leaderboard not-signed-in body contrast ($bodyContrast) must be >= 7.0:1 (AAA)", bodyContrast >= 7.0)
    }

    @Test
    fun testLevelHistoryEmptyState_lightModeContrastCompliance() {
        val scheme = DefaultLightColorScheme
        // LevelHistoryScreen empty state uses onSurface over surface
        val textContrast = calculateContrastRatio(scheme.onSurface, scheme.surface)
        assertTrue("LevelHistoryScreen empty state text contrast ($textContrast) must be >= 7.0:1 (AAA)", textContrast >= 7.0)
    }
}
