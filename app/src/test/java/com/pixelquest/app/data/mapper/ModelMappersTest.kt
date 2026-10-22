package com.pixelquest.app.data.mapper

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.remote.model.ReportDto
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ModelMappersTest {

    @Test
    fun taskEntity_and_domainModel_roundTrip() {
        val entity = TaskEntity(
            id = 42L,
            name = "Morning Quest",
            description = "Run 5km",
            scheduledDay = LocalDate.of(2026, 10, 23),
            scheduledTime = LocalTime.of(7, 30),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
            isActive = true,
            createdAt = 123456789L,
            reminderEnabled = true,
            reminderLeadMinutes = 15,
            reminderStyle = ReminderStyle.STANDARD,
            weeklyDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )

        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.description, domain.description)
        assertEquals(entity.scheduledDay, domain.scheduledDay)
        assertEquals(entity.scheduledTime, domain.scheduledTime)
        assertEquals(entity.recurrenceType, domain.recurrenceType)
        assertEquals(entity.category, domain.category)
        assertEquals(entity.isActive, domain.isActive)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.reminderEnabled, domain.reminderEnabled)
        assertEquals(entity.reminderLeadMinutes, domain.reminderLeadMinutes)
        assertEquals(entity.reminderStyle, domain.reminderStyle)
        assertEquals(entity.weeklyDays, domain.weeklyDays)

        val backToEntity = domain.toEntity()
        assertEquals(entity, backToEntity)
    }

    @Test
    fun userProfileEntity_and_domainModel_roundTrip() {
        val entity = UserProfileEntity(
            id = 1L,
            username = "HeroCoder",
            avatarId = "avatar_mage",
            level = 10,
            totalXp = 5000,
            perfectDaysTowardNextLevel = 4,
            createdAt = 987654321L,
            supabaseUserId = "user-uuid-123",
            leaderboardOptIn = true,
            leaderboardDisplayName = "MageHero",
        )

        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.username, domain.username)
        assertEquals(entity.avatarId, domain.avatarId)
        assertEquals(entity.level, domain.level)
        assertEquals(entity.totalXp, domain.totalXp)
        assertEquals(entity.perfectDaysTowardNextLevel, domain.perfectDaysTowardNextLevel)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.supabaseUserId, domain.supabaseUserId)
        assertEquals(entity.leaderboardOptIn, domain.leaderboardOptIn)
        assertEquals(entity.leaderboardDisplayName, domain.leaderboardDisplayName)

        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun streakEntity_and_domainModel_roundTrip() {
        val entity = StreakEntity(
            id = 1L,
            currentStreak = 14,
            longestStreak = 30,
            lastCompletedDate = LocalDate.of(2026, 10, 22),
        )

        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.currentStreak, domain.currentStreak)
        assertEquals(entity.longestStreak, domain.longestStreak)
        assertEquals(entity.lastCompletedDate, domain.lastCompletedDate)

        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun difficultySettingsEntity_and_domainModel_roundTrip() {
        val entity = DifficultySettingsEntity(
            id = 1L,
            difficultyLevel = DifficultyLevel.HARD,
            perfectDayThreshold = 0.85f,
            daysRequiredPerLevel = 10,
        )

        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.difficultyLevel, domain.difficultyLevel)
        assertEquals(entity.perfectDayThreshold, domain.perfectDayThreshold, 0.001f)
        assertEquals(entity.daysRequiredPerLevel, domain.daysRequiredPerLevel)

        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun levelHistoryEntity_and_domainModel_roundTrip() {
        val entity = LevelHistoryEntity(
            id = 5L,
            level = 11,
            achievedDate = 1700000000L,
            difficultyAtTimeOfLevelUp = "HARD",
        )

        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.level, domain.level)
        assertEquals(entity.achievedDate, domain.achievedDate)
        assertEquals(entity.difficultyAtTimeOfLevelUp, domain.difficultyAtTimeOfLevelUp)

        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun taskCompletionLogEntity_and_domainModel_roundTrip() {
        val entity = TaskCompletionLogEntity(
            id = 12L,
            taskId = 42L,
            completedDate = LocalDate.of(2026, 10, 23),
            wasCompleted = true,
            pointsAwarded = 50,
        )

        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.taskId, domain.taskId)
        assertEquals(entity.completedDate, domain.completedDate)
        assertEquals(entity.wasCompleted, domain.wasCompleted)
        assertEquals(entity.pointsAwarded, domain.pointsAwarded)

        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun cloudProfileDto_and_domainModel_roundTrip() {
        val dto = CloudProfileDto(
            id = "uuid-99",
            displayName = "Champion",
            currentStreak = 25,
            longestStreak = 50,
            level = 15,
            totalXp = 12500,
            leaderboardOptIn = true,
            updatedAt = "2026-10-23T00:00:00Z",
        )

        val domain = dto.toDomain()
        assertEquals(dto.id, domain.id)
        assertEquals(dto.displayName, domain.displayName)
        assertEquals(dto.currentStreak, domain.currentStreak)
        assertEquals(dto.longestStreak, domain.longestStreak)
        assertEquals(dto.level, domain.level)
        assertEquals(dto.totalXp, domain.totalXp)
        assertEquals(dto.leaderboardOptIn, domain.leaderboardOptIn)
        assertEquals(dto.updatedAt, domain.updatedAt)

        assertEquals(dto, domain.toDto())
    }

    @Test
    fun reportDto_and_domainModel_roundTrip() {
        val dto = ReportDto(
            reporterId = "user-1",
            reportedProfileId = "user-2",
            reason = "Inappropriate name",
        )

        val domain = dto.toDomain()
        assertEquals(dto.reporterId, domain.reporterId)
        assertEquals(dto.reportedProfileId, domain.reportedProfileId)
        assertEquals(dto.reason, domain.reason)

        assertEquals(dto, domain.toDto())
    }
}
