package com.pixelquest.app.ui.components

import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelBackgroundCard
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelYellow
import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapColorMapperTest {

    @Test
    fun getCellColor_perfectDay_returnsPixelGreen() {
        val color = HeatmapColorMapper.getCellColor(DailyStatus.PERFECT)
        assertEquals(PixelGreen, color)
    }

    @Test
    fun getCellColor_partialDay_returnsPixelYellow() {
        val color = HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL)
        assertEquals(PixelYellow, color)
    }

    @Test
    fun getCellColor_missedDay_returnsPixelRed() {
        val color = HeatmapColorMapper.getCellColor(DailyStatus.MISSED)
        assertEquals(PixelRed, color)
    }

    @Test
    fun getCellColor_noTasksScheduled_returnsBackgroundCard() {
        val color = HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED)
        assertEquals(PixelBackgroundCard, color)
    }
}
