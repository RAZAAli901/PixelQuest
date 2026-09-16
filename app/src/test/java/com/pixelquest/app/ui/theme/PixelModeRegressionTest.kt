package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.HeatmapColorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Step 38: Regression test verifying Pixel (Retro Dark Arcade) mode remains 100%
 * identical and completely unaffected by Day 17's light-mode work.
 */
class PixelModeRegressionTest {

    @Test
    fun testDefaultPixelColorScheme_remainsExactCanonicalValues() {
        val scheme = DefaultPixelColorScheme

        assertEquals("Background must be canonical #12121E", Color(0xFF12121E), scheme.background)
        assertEquals("Surface must be canonical #1A1A2E", Color(0xFF1A1A2E), scheme.surface)
        assertEquals("SurfaceVariant must be canonical #252538", Color(0xFF252538), scheme.surfaceVariant)
        assertEquals("Primary must be canonical PixelGold (#FFD700)", PixelGold, scheme.primary)
        assertEquals("Secondary must be canonical PixelCyan (#00E5FF)", PixelCyan, scheme.secondary)
        assertEquals("Tertiary must be canonical PixelGreen (#00E676)", PixelGreen, scheme.tertiary)
        assertEquals("Error must be canonical PixelRed (#FF5252)", PixelRed, scheme.error)
        assertEquals("OnBackground must be Color.White", Color.White, scheme.onBackground)
        assertEquals("OnSurface must be Color.White", Color.White, scheme.onSurface)
        assertEquals("Gold must be canonical PixelGold", PixelGold, scheme.gold)
    }

    @Test
    fun testPixelModeAssetFilter_returnsNullForNoTinting() {
        TaskCategory.values().forEach { category ->
            val filter = PixelThemeAssetFilter.getCategoryColorFilter(
                category = category,
                themeMode = ThemeMode.Pixel,
                isSystemInDarkTheme = false
            )
            assertNull("Pixel theme must not tint category ${category.name}", filter)
        }
    }

    @Test
    fun testPixelModeHeatmapRamp_remainsCanonicalDarkColors() {
        assertEquals(PixelGreen, HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, isLight = false))
        assertEquals(PixelGold, HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, isLight = false))
        assertEquals(PixelRed, HeatmapColorMapper.getCellColor(DailyStatus.MISSED, isLight = false))
        assertEquals(PixelSurfaceDark, HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = false))
    }

    @Test
    fun testPixelModeEffectiveResolution_isAlwaysPixelMode() {
        assertEquals(ThemeMode.Pixel, ThemeMode.Pixel.resolveEffective(isSystemInDarkTheme = false))
        assertEquals(ThemeMode.Pixel, ThemeMode.Pixel.resolveEffective(isSystemInDarkTheme = true))
    }
}
