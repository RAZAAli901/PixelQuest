package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PointsCalculatorTest {

    @Test
    fun calculateXpForTask_zeroStreak_returnsBaseXp() {
        val xp = PointsCalculator.calculateXpForTask(currentStreak = 0)
        assertEquals(50, xp)
    }

    @Test
    fun calculateXpForTask_lowStreak_returnsBasePlusStreakBonus() {
        // 3 days streak -> 50 + (3 * 10) = 80 XP
        val xp = PointsCalculator.calculateXpForTask(currentStreak = 3)
        assertEquals(80, xp)
    }

    @Test
    fun calculateXpForTask_highStreak_returnsBasePlusLargeStreakBonus() {
        // 10 days streak -> 50 + (10 * 10) = 150 XP
        val xp = PointsCalculator.calculateXpForTask(currentStreak = 10)
        assertEquals(150, xp)
    }

    @Test
    fun calculateXpForTask_negativeStreak_coercesToZeroBonus() {
        val xp = PointsCalculator.calculateXpForTask(currentStreak = -5)
        assertEquals(50, xp)
    }
}
