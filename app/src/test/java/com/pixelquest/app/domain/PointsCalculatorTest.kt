package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PointsCalculatorTest {

    @Test
    fun calculateXpForTask_returnsBaseXp() {
        val xp = PointsCalculator.calculateXpForTask()
        assertEquals(50, xp)
        assertEquals(PointsCalculator.BASE_XP_PER_TASK, xp)
    }
}
