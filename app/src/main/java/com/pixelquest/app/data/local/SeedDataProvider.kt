package com.pixelquest.app.data.local

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import java.time.LocalDate
import java.time.LocalTime

object SeedDataProvider {

    fun defaultProfile(): UserProfileEntity {
        return UserProfileEntity(
            id = 1,
            username = "PixelHero",
            avatarId = "hero_avatar_1",
            level = 1,
            totalXp = 0
        )
    }

    fun defaultDifficultySettings(): DifficultySettingsEntity {
        return DifficultySettingsEntity(
            id = 1,
            difficultyLevel = DifficultyLevel.MEDIUM,
            perfectDayThreshold = 0.8f,
            daysRequiredPerLevel = 7
        )
    }

    fun defaultStreak(): StreakEntity {
        return StreakEntity(
            id = 1,
            currentStreak = 0,
            longestStreak = 0,
            lastCompletedDate = null,
            perfectDaysCount = 0
        )
    }

    fun initialTasks(): List<TaskEntity> {
        val today = LocalDate.now()
        return listOf(
            TaskEntity(
                id = 1,
                name = "20 Push-ups",
                description = "Daily physical conditioning quest",
                scheduledDay = today,
                scheduledTime = LocalTime.of(8, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            ),
            TaskEntity(
                id = 2,
                name = "Go to Gym",
                description = "Strength training session",
                scheduledDay = today,
                scheduledTime = LocalTime.of(17, 30),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            ),
            TaskEntity(
                id = 3,
                name = "Read 15 Pages",
                description = "Knowledge expansion habit",
                scheduledDay = today,
                scheduledTime = LocalTime.of(21, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.PRODUCTIVITY
            )
        )
    }
}
