package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakIntegrationTest {

    @Test
    fun simulateThreeConsecutivePerfectDays_incrementsStreakTo3AndScalesXp() {
        var currentStreak = 0
        var totalXp = 0

        // Day 1
        val day1Perfect = StreakCalculator.isPerfectDay(1, 1, 0.70f)
        if (day1Perfect) {
            currentStreak++
            totalXp += PointsCalculator.calculateXpForTask(streakDays = currentStreak)
        }

        assertEquals(1, currentStreak)
        assertEquals(60, totalXp) // 50 + 10 = 60

        // Day 2
        val day2Perfect = StreakCalculator.isPerfectDay(1, 1, 0.70f)
        if (day2Perfect) {
            currentStreak++
            totalXp += PointsCalculator.calculateXpForTask(streakDays = currentStreak)
        }

        assertEquals(2, currentStreak)
        assertEquals(130, totalXp) // 60 + (50 + 20) = 130

        // Day 3
        val day3Perfect = StreakCalculator.isPerfectDay(1, 1, 0.70f)
        if (day3Perfect) {
            currentStreak++
            totalXp += PointsCalculator.calculateXpForTask(streakDays = currentStreak)
        }

        assertEquals(3, currentStreak)
        assertEquals(210, totalXp) // 130 + (50 + 30) = 210
    }
}
