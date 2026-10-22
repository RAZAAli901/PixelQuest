package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class FakeRepositoriesContractTest {

    @Test
    fun fakeTaskRepository_crudAndOccurrenceContract() = runBlocking {
        val repo = FakeTaskRepository()
        val monday = LocalDate.of(2026, 10, 19) // Monday
        val tuesday = LocalDate.of(2026, 10, 20) // Tuesday

        val dailyTask = TaskEntity(
            id = 0,
            name = "Daily Pushups",
            description = "Stay strong",
            category = TaskCategory.FITNESS,
            scheduledDay = monday,
            scheduledTime = LocalTime.of(7, 0),
            recurrenceType = RecurrenceType.DAILY,
        )
        val weeklyTask = TaskEntity(
            id = 0,
            name = "Weekly Review",
            description = "Plan ahead",
            category = TaskCategory.LEARNING,
            scheduledDay = monday,
            scheduledTime = LocalTime.of(18, 0),
            recurrenceType = RecurrenceType.WEEKLY,
            weeklyDays = setOf(DayOfWeek.MONDAY),
        )
        val oneTimeTask = TaskEntity(
            id = 0,
            name = "Dentist",
            description = "Checkup",
            category = TaskCategory.HEALTH,
            scheduledDay = monday,
            scheduledTime = LocalTime.of(14, 0),
            recurrenceType = RecurrenceType.ONE_TIME,
        )

        val id1 = repo.insertTask(dailyTask)
        val id2 = repo.insertTask(weeklyTask)
        val id3 = repo.insertTask(oneTimeTask)

        assertTrue(id1 > 0)
        assertTrue(id2 > id1)
        assertTrue(id3 > id2)

        val all = repo.getAllTasks().first()
        assertEquals(3, all.size)

        // Monday should have all three
        val mondayTasks = repo.getTasksForDay(monday).first()
        assertEquals(3, mondayTasks.size)
        // Verify sorted by scheduledTime: 7:00, 14:00, 18:00
        assertEquals("Daily Pushups", mondayTasks[0].name)
        assertEquals("Dentist", mondayTasks[1].name)
        assertEquals("Weekly Review", mondayTasks[2].name)

        // Tuesday should only have the daily task
        val tuesdayTasks = repo.getTasksForDay(tuesday).first()
        assertEquals(1, tuesdayTasks.size)
        assertEquals("Daily Pushups", tuesdayTasks[0].name)

        // Update task
        val updated = mondayTasks[0].copy(name = "Updated Pushups")
        repo.updateTask(updated)
        assertEquals("Updated Pushups", repo.getTaskById(id1).first()?.name)

        // Delete task
        repo.deleteTask(updated)
        assertEquals(2, repo.getAllTasks().first().size)
        assertNull(repo.getTaskById(id1).first())
    }

    @Test
    fun fakeTaskCompletionRepository_uniqueDayConstraintAndHistory() = runBlocking {
        val repo = FakeTaskCompletionRepository()
        val date1 = LocalDate.of(2026, 10, 20)
        val date2 = LocalDate.of(2026, 10, 21)

        val log1 = TaskCompletionLogEntity(
            id = 0,
            taskId = 10,
            completedDate = date1,
            wasCompleted = true,
            pointsAwarded = 10,
        )
        val logDuplicate = TaskCompletionLogEntity(
            id = 0,
            taskId = 10,
            completedDate = date1,
            wasCompleted = false,
            pointsAwarded = 0,
        )
        val logDifferentDay = TaskCompletionLogEntity(
            id = 0,
            taskId = 10,
            completedDate = date2,
            wasCompleted = true,
            pointsAwarded = 15,
        )

        val id1 = repo.insertLog(log1)
        assertTrue(id1 > 0)

        // Duplicate insert on same day for same task returns -1
        val duplicateId = repo.insertLog(logDuplicate)
        assertEquals(-1L, duplicateId)

        val id2 = repo.insertLog(logDifferentDay)
        assertTrue(id2 > 0)

        // Query by date
        val logsOnDate1 = repo.getLogsForDate(date1).first()
        assertEquals(1, logsOnDate1.size)
        assertTrue(logsOnDate1[0].wasCompleted)

        // Query history in range
        val history = repo.getCompletionHistory(date1, date2).first()
        assertEquals(2, history.size)
    }

    @Test
    fun fakeUserProfileRepository_levelUpAndCloudSettings() = runBlocking {
        val initial = UserProfileEntity(id = 1, username = "PixelKnight", avatarId = "avatar_hero", level = 5, totalXp = 1000)
        val repo = FakeUserProfileRepository(initial)

        val profile = repo.getProfile().first()
        assertNotNull(profile)
        assertEquals("PixelKnight", profile?.username)
        assertEquals(5, profile?.level)

        // Level up
        val leveled = repo.performLevelUp()
        assertNotNull(leveled)
        assertEquals(6, leveled?.level)
        assertEquals(6, repo.getProfile().first()?.level)

        // Cloud settings
        repo.updateSupabaseUserId("user-123")
        repo.updateLeaderboardSettings(optIn = true, displayName = "Champion")
        val updated = repo.getProfile().first()
        assertEquals("user-123", updated?.supabaseUserId)
        assertTrue(updated?.leaderboardOptIn == true)
        assertEquals("Champion", updated?.leaderboardDisplayName)

        // Clear cloud data
        repo.clearCloudData()
        val cleared = repo.getProfile().first()
        assertNull(cleared?.supabaseUserId)
        assertFalse(cleared?.leaderboardOptIn == true)
        assertNull(cleared?.leaderboardDisplayName)
    }

    @Test
    fun fakeStreakAndSettingsRepositories_statePropagation() = runBlocking {
        val streakRepo = FakeStreakRepository(StreakEntity(id = 1, currentStreak = 7, longestStreak = 14))
        assertEquals(7, streakRepo.getCurrentStreak().first()?.currentStreak)

        streakRepo.updateStreak(StreakEntity(id = 1, currentStreak = 8, longestStreak = 14))
        assertEquals(8, streakRepo.getCurrentStreak().first()?.currentStreak)

        val settingsRepo = FakeSettingsRepository()
        assertFalse(settingsRepo.simpleModeEnabled.first())
        settingsRepo.setSimpleModeEnabled(true)
        assertTrue(settingsRepo.simpleModeEnabled.first())

        assertEquals(ThemeMode.Pixel, settingsRepo.themeMode.first())
        settingsRepo.setThemeMode(ThemeMode.Comic)
        assertEquals(ThemeMode.Comic, settingsRepo.themeMode.first())
    }
}
