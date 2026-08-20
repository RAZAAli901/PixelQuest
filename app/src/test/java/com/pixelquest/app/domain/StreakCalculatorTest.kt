package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class StreakCalculatorTest {

    @Test
    fun calculateCompletionPercentage_zeroTasks_returnsOneHundredPercent() {
        val pct = StreakCalculator.calculateCompletionPercentage(0, 0)
        assertEquals(1.0f, pct, 0.001f)
    }

    @Test
    fun calculateCompletionPercentage_singleTaskCompleted_returnsOneHundredPercent() {
        val pct = StreakCalculator.calculateCompletionPercentage(1, 1)
        assertEquals(1.0f, pct, 0.001f)
    }

    @Test
    fun calculateCompletionPercentage_partialCompletion_returnsCorrectRatio() {
        val pct = StreakCalculator.calculateCompletionPercentage(3, 4)
        assertEquals(0.75f, pct, 0.001f)
    }

    @Test
    fun isPerfectDay_meetingOrExceedingThreshold_returnsTrue() {
        assertTrue(StreakCalculator.isPerfectDay(0.75f, 0.70f))
        assertTrue(StreakCalculator.isPerfectDay(0.70f, 0.70f))
    }

    @Test
    fun isPerfectDay_belowThreshold_returnsFalse() {
        assertFalse(StreakCalculator.isPerfectDay(0.69f, 0.70f))
        assertFalse(StreakCalculator.isPerfectDay(0.50f, 0.90f))
    }

    @Test
    fun isPerfectDay_withLogsList_calculatesCorrectly() {
        val logs = listOf(
            TaskCompletionLogEntity(taskId = 1, completedAt = LocalDateTime.now(), wasCompleted = true),
            TaskCompletionLogEntity(taskId = 2, completedAt = LocalDateTime.now(), wasCompleted = true),
            TaskCompletionLogEntity(taskId = 3, completedAt = LocalDateTime.now(), wasCompleted = false)
        )
        // 2 out of 3 = 66.6% -> should pass 50% threshold, fail 70% threshold
        assertTrue(StreakCalculator.isPerfectDay(logs, 3, 0.50f))
        assertFalse(StreakCalculator.isPerfectDay(logs, 3, 0.70f))
    }
}
