package com.pixelquest.app.ui.stats

import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.screens.stats.StatsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SimpleModeStatsIntegrityTest {

    @Test
    fun `completion rate and task history are maintained in simple mode state`() {
        val state = StatsUiState(
            currentStreak = 14,
            longestStreak = 21,
            totalPoints = 1400,
            overallCompletionRate = 0.88f,
            difficultyLevel = DifficultyLevel.MEDIUM,
            heatmapStatusMap = mapOf(LocalDate.now() to DailyStatus.PERFECT),
            weeklyTrend = listOf("NOW" to 0.88f),
            isSimpleMode = true
        )

        // Verify completion rate remains accurate and neutral
        assertEquals(0.88f, state.overallCompletionRate, 0.001f)
        assertTrue(state.isSimpleMode)

        // Verify underlying streak and points are still present in data state for background tracking
        assertEquals(14, state.currentStreak)
        assertEquals(21, state.longestStreak)
        assertEquals(1400, state.totalPoints)
    }

    @Test
    fun `gamified mode state preserves all metrics`() {
        val state = StatsUiState(
            currentStreak = 5,
            longestStreak = 10,
            totalPoints = 650,
            overallCompletionRate = 0.75f,
            isSimpleMode = false
        )

        assertEquals(0.75f, state.overallCompletionRate, 0.001f)
        assertEquals(5, state.currentStreak)
        assertEquals(10, state.longestStreak)
        assertEquals(650, state.totalPoints)
        org.junit.Assert.assertFalse(state.isSimpleMode)
    }
}
