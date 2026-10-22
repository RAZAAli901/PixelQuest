package com.pixelquest.app.data.mapper

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultySettings
import com.pixelquest.app.domain.model.LevelHistory
import com.pixelquest.app.domain.model.Streak
import com.pixelquest.app.domain.model.Task
import com.pixelquest.app.domain.model.TaskCompletionLog
import com.pixelquest.app.domain.model.UserProfile

/**
 * Bidirectional mappers between Room database entities and domain models (Q042).
 */

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    name = name,
    description = description,
    scheduledDay = scheduledDay,
    scheduledTime = scheduledTime,
    recurrenceType = recurrenceType,
    category = category,
    isActive = isActive,
    createdAt = createdAt,
    reminderEnabled = reminderEnabled,
    reminderLeadMinutes = reminderLeadMinutes,
    reminderStyle = reminderStyle,
    weeklyDays = weeklyDays,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    description = description,
    scheduledDay = scheduledDay,
    scheduledTime = scheduledTime,
    recurrenceType = recurrenceType,
    category = category,
    isActive = isActive,
    createdAt = createdAt,
    reminderEnabled = reminderEnabled,
    reminderLeadMinutes = reminderLeadMinutes,
    reminderStyle = reminderStyle,
    weeklyDays = weeklyDays,
)

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    avatarId = avatarId,
    level = level,
    totalXp = totalXp,
    perfectDaysTowardNextLevel = perfectDaysTowardNextLevel,
    createdAt = createdAt,
    supabaseUserId = supabaseUserId,
    leaderboardOptIn = leaderboardOptIn,
    leaderboardDisplayName = leaderboardDisplayName,
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    username = username,
    avatarId = avatarId,
    level = level,
    totalXp = totalXp,
    perfectDaysTowardNextLevel = perfectDaysTowardNextLevel,
    createdAt = createdAt,
    supabaseUserId = supabaseUserId,
    leaderboardOptIn = leaderboardOptIn,
    leaderboardDisplayName = leaderboardDisplayName,
)

fun StreakEntity.toDomain(): Streak = Streak(
    id = id,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastCompletedDate = lastCompletedDate,
)

fun Streak.toEntity(): StreakEntity = StreakEntity(
    id = id,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastCompletedDate = lastCompletedDate,
)

fun DifficultySettingsEntity.toDomain(): DifficultySettings = DifficultySettings(
    id = id,
    difficultyLevel = difficultyLevel,
    perfectDayThreshold = perfectDayThreshold,
    daysRequiredPerLevel = daysRequiredPerLevel,
)

fun DifficultySettings.toEntity(): DifficultySettingsEntity = DifficultySettingsEntity(
    id = id,
    difficultyLevel = difficultyLevel,
    perfectDayThreshold = perfectDayThreshold,
    daysRequiredPerLevel = daysRequiredPerLevel,
)

fun LevelHistoryEntity.toDomain(): LevelHistory = LevelHistory(
    id = id,
    level = level,
    achievedDate = achievedDate,
    difficultyAtTimeOfLevelUp = difficultyAtTimeOfLevelUp,
)

fun LevelHistory.toEntity(): LevelHistoryEntity = LevelHistoryEntity(
    id = id,
    level = level,
    achievedDate = achievedDate,
    difficultyAtTimeOfLevelUp = difficultyAtTimeOfLevelUp,
)

fun TaskCompletionLogEntity.toDomain(): TaskCompletionLog = TaskCompletionLog(
    id = id,
    taskId = taskId,
    completedDate = completedDate,
    wasCompleted = wasCompleted,
    pointsAwarded = pointsAwarded,
)

fun TaskCompletionLog.toEntity(): TaskCompletionLogEntity = TaskCompletionLogEntity(
    id = id,
    taskId = taskId,
    completedDate = completedDate,
    wasCompleted = wasCompleted,
    pointsAwarded = pointsAwarded,
)
