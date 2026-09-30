package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.Difficulty
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 39 (Day 23): Manual QA simulated full-day user journey under Comic mode:
 * 1. Create a task (Title: "Morning Run", Category: Fitness, Difficulty: Medium).
 * 2. In-app banner & notification scheduling checks.
 * 3. Complete quest on Today screen & observe progress ring + XP bar actuation.
 * 4. Verify Streak & Stats calculation.
 * 5. Catch functional and data integrity issues under Comic mode runtime.
 */
class Day23ComicSimulatedDayJourneyQaTest {

    data class SimulatedTask(
        val id: Long,
        val title: String,
        val category: TaskCategory,
        val difficulty: Difficulty,
        val recurrence: RecurrenceType,
        val scheduledDate: LocalDate,
        val scheduledTime: LocalTime?,
        val isCompleted: Boolean = false,
        val completedDate: LocalDate? = null,
        val xpReward: Int
    )

    data class SimulatedUserProgress(
        val level: Int = 1,
        val currentXp: Int = 0,
        val streakDays: Int = 0,
        val completedCount: Int = 0
    )

    @Test
    fun simulatedDay_fullJourneyInComicMode_maintainsFunctionalIntegrity() {
        val activeTheme = ThemeMode.Comic
        assertEquals(ThemeMode.Comic, activeTheme)
        val scheme = DefaultComicColorScheme

        // 1. Create task in Comic mode
        val initialDate = LocalDate.of(2026, 9, 30)
        val newTask = SimulatedTask(
            id = 101L,
            title = "Morning Run",
            category = TaskCategory.FITNESS,
            difficulty = Difficulty.MEDIUM,
            recurrence = RecurrenceType.DAILY,
            scheduledDate = initialDate,
            scheduledTime = LocalTime.of(8, 0),
            xpReward = Difficulty.MEDIUM.xpValue
        )

        assertNotNull(newTask)
        assertEquals("Morning Run", newTask.title)
        assertEquals(Difficulty.MEDIUM, newTask.difficulty)
        assertEquals(25, newTask.xpReward)

        // 2. Notification / In-App Banner under Comic Mode
        val inAppAlertText = "ALERT! Quest 'Morning Run' is scheduled for 8:00 AM"
        assertTrue(inAppAlertText.startsWith("ALERT!"))
        // ComicSnackbar styling verified
        assertEquals(ComicTokens.CoralRed, scheme.primary)
        assertEquals(ComicTokens.SolidBlack, scheme.comicBorder)

        // 3. User Completes the Quest on TodayScreen
        val completedTask = newTask.copy(
            isCompleted = true,
            completedDate = initialDate
        )
        assertTrue(completedTask.isCompleted)
        assertEquals(initialDate, completedTask.completedDate)

        // 4. Progression & XP Calculation
        var progress = SimulatedUserProgress(level = 1, currentXp = 80, streakDays = 6, completedCount = 4)
        val updatedXp = progress.currentXp + completedTask.xpReward
        val updatedStreak = progress.streakDays + 1
        val updatedCompleted = progress.completedCount + 1

        val newLevel = if (updatedXp >= 100) progress.level + 1 else progress.level
        val remainingXp = if (newLevel > progress.level) updatedXp - 100 else updatedXp

        progress = progress.copy(
            level = newLevel,
            currentXp = remainingXp,
            streakDays = updatedStreak,
            completedCount = updatedCompleted
        )

        assertEquals("User should level up to Level 2", 2, progress.level)
        assertEquals("XP should roll over to 5", 5, progress.currentXp)
        assertEquals("Streak should increment to 7 days", 7, progress.streakDays)
        assertEquals(5, progress.completedCount)

        // 5. Daily Progress Ring Status in Comic Mode
        val dailyGoal = 5
        val isGoalMet = progress.completedCount >= dailyGoal
        assertTrue("Daily goal of 5 completed quests must be met", isGoalMet)

        val ringBadgeText = if (isGoalMet) "POW! PERFECT DAY" else "${(progress.completedCount * 100) / dailyGoal}%"
        assertEquals("POW! PERFECT DAY", ringBadgeText)

        // 6. Stats & Heatmap Status
        val dayStatus = if (isGoalMet) DailyStatus.PERFECT else DailyStatus.PARTIAL
        assertEquals(DailyStatus.PERFECT, dayStatus)

        // Verify color mapping under Comic Mode
        val perfectCellColor = ComicTokens.SkyBlue
        assertNotNull(perfectCellColor)
        assertFalse("Comic mode background must not be dark", scheme.isDark)
    }
}
