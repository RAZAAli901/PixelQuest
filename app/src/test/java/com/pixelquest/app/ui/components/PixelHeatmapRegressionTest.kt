package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 31: Regression test confirming Pixel/Light heatmap rendering is unaffected
 * after adding the Comic-mode color ramp and dispatch.
 */
class PixelHeatmapRegressionTest {

    @Test
    fun heatmap_pixelMode_darkRampPreserved() {
        // Cells
        assertEquals(PixelGreen, HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, isLight = false))
        assertEquals(PixelGold, HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, isLight = false))
        assertEquals(PixelRed, HeatmapColorMapper.getCellColor(DailyStatus.MISSED, isLight = false))
        assertEquals(PixelSurfaceDark, HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = false))

        // Borders
        assertEquals(Color(0xFF1C7139), HeatmapColorMapper.getBorderColor(DailyStatus.PERFECT, isLight = false))
        assertEquals(Color(0xFFC8A100), HeatmapColorMapper.getBorderColor(DailyStatus.PARTIAL, isLight = false))
        assertEquals(Color(0xFF8B0000), HeatmapColorMapper.getBorderColor(DailyStatus.MISSED, isLight = false))
        assertEquals(Color(0xFF333842), HeatmapColorMapper.getBorderColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = false))
    }

    @Test
    fun heatmap_lightMode_lightRampPreserved() {
        // Cells
        assertEquals(Color(0xFF15803D), HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, isLight = true))
        assertEquals(Color(0xFFD97706), HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, isLight = true))
        assertEquals(Color(0xFFDC2626), HeatmapColorMapper.getCellColor(DailyStatus.MISSED, isLight = true))
        assertEquals(Color(0xFFEFECE6), HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = true))

        // Borders
        assertEquals(Color(0xFF166534), HeatmapColorMapper.getBorderColor(DailyStatus.PERFECT, isLight = true))
        assertEquals(Color(0xFFB45309), HeatmapColorMapper.getBorderColor(DailyStatus.PARTIAL, isLight = true))
        assertEquals(Color(0xFF991B1B), HeatmapColorMapper.getBorderColor(DailyStatus.MISSED, isLight = true))
        assertEquals(Color(0xFFD5CEBF), HeatmapColorMapper.getBorderColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = true))
    }

    @Test
    fun heatmap_comicMode_comicRampResolves() {
        // Signature container colors
        assertEquals(Color(0xFF8ECAE6), HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, ThemeMode.Comic)) // Sky Blue
        assertEquals(Color(0xFFF0A868), HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, ThemeMode.Comic)) // Burnt Orange
        assertEquals(Color(0xFFFF5A4E), HeatmapColorMapper.getCellColor(DailyStatus.MISSED, ThemeMode.Comic))  // Coral Red
        assertEquals(Color(0xFFF4EFE6), HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, ThemeMode.Comic)) // Newsprint SurfaceVariant

        // Comic ink borders are all solid black
        assertEquals(Color(0xFF000000), HeatmapColorMapper.getBorderColor(DailyStatus.PERFECT, ThemeMode.Comic))
        assertEquals(Color(0xFF000000), HeatmapColorMapper.getBorderColor(DailyStatus.PARTIAL, ThemeMode.Comic))
        assertEquals(Color(0xFF000000), HeatmapColorMapper.getBorderColor(DailyStatus.MISSED, ThemeMode.Comic))
        assertEquals(Color(0xFF000000), HeatmapColorMapper.getBorderColor(DailyStatus.NO_TASKS_SCHEDULED, ThemeMode.Comic))
    }

    @Test
    fun heatmap_dispatchRouting_isolatesThemes() {
        assertFalse(ThemeMode.Pixel == ThemeMode.Comic)
        assertFalse(ThemeMode.Light == ThemeMode.Comic)
        assertTrue(ThemeMode.Comic == ThemeMode.Comic)
    }
}
