package com.pixelquest.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelXpBarRenderTest {

    @Test
    fun testXpBarCalculationBounds() {
        val currentProgress = 2
        val maxProgress = 3
        val progressFraction = (currentProgress.toFloat() / maxProgress.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.6666667f, progressFraction, 0.001f)
    }

    @Test
    fun testLevelBadgeFormatting() {
        val level = 5
        val levelBadgeText = "LVL $level"
        assertEquals("LVL 5", levelBadgeText)
    }
}
