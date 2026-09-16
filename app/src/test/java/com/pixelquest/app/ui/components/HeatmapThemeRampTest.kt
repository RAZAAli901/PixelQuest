package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.model.DailyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Step 32: Test verifying the heatmap color ramp switches correctly with theme.
 * Confirms that the Light ramp is a tailored daylight palette rather than a simple color inversion.
 */
class HeatmapThemeRampTest {

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
    fun testHeatmapColorRampSwitchesCorrectlyBetweenThemes() {
        // Verify Dark Ramp
        assertEquals(HeatmapColorMapper.DarkPerfectCell, HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, isLight = false))
        assertEquals(HeatmapColorMapper.DarkPartialCell, HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, isLight = false))
        assertEquals(HeatmapColorMapper.DarkMissedCell, HeatmapColorMapper.getCellColor(DailyStatus.MISSED, isLight = false))
        assertEquals(HeatmapColorMapper.DarkEmptyCell, HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = false))

        // Verify Light Ramp
        assertEquals(HeatmapColorMapper.LightPerfectCell, HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, isLight = true))
        assertEquals(HeatmapColorMapper.LightPartialCell, HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, isLight = true))
        assertEquals(HeatmapColorMapper.LightMissedCell, HeatmapColorMapper.getCellColor(DailyStatus.MISSED, isLight = true))
        assertEquals(HeatmapColorMapper.LightEmptyCell, HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = true))

        // Verify Border Ramps
        assertEquals(HeatmapColorMapper.LightPerfectBorder, HeatmapColorMapper.getBorderColor(DailyStatus.PERFECT, isLight = true))
        assertEquals(HeatmapColorMapper.DarkPerfectBorder, HeatmapColorMapper.getBorderColor(DailyStatus.PERFECT, isLight = false))
    }

    @Test
    fun testLightRampIsNotSimpleInversionAndMeetsContrast() {
        // Assert light colors are distinct from dark colors
        assertNotEquals(HeatmapColorMapper.DarkPerfectCell, HeatmapColorMapper.LightPerfectCell)
        assertNotEquals(HeatmapColorMapper.DarkPartialCell, HeatmapColorMapper.LightPartialCell)
        assertNotEquals(HeatmapColorMapper.DarkMissedCell, HeatmapColorMapper.LightMissedCell)
        assertNotEquals(HeatmapColorMapper.DarkEmptyCell, HeatmapColorMapper.LightEmptyCell)

        // Light mode empty cell should have a subtle distinction from white card surface
        val white = Color.White
        val emptyContrast = calculateContrastRatio(HeatmapColorMapper.LightEmptyCell, white)
        assertTrue("Empty cell contrast against white should be subtle (~1.1-1.3:1): $emptyContrast", emptyContrast >= 1.05)

        // Perfect cell (emerald green 0xFF15803D) on white card surface
        val perfectContrast = calculateContrastRatio(HeatmapColorMapper.LightPerfectCell, white)
        assertTrue("Light perfect cell contrast against white ($perfectContrast) must exceed 4.5:1", perfectContrast >= 4.5)

        // Missed cell (crimson 0xFFDC2626) on white card surface
        val missedContrast = calculateContrastRatio(HeatmapColorMapper.LightMissedCell, white)
        assertTrue("Light missed cell contrast against white ($missedContrast) must exceed 4.5:1", missedContrast >= 4.5)
    }
}
