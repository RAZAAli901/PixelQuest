package com.pixelquest.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pure domain models separated from Room database entities and remote DTOs (Q042).
 */

data class Task(
    val id: Long = 0,
    val name: String,
    val description: String,
    val scheduledDay: LocalDate,
    val scheduledTime: LocalTime,
    val recurrenceType: RecurrenceType,
    val category: TaskCategory,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 0,
    val reminderStyle: ReminderStyle = ReminderStyle.STANDARD,
    val weeklyDays: Set<DayOfWeek> = emptySet(),
)

data class UserProfile(
    val id: Long = 1,
    val username: String,
    val avatarId: String,
    val level: Int = 1,
    val totalXp: Int = 0,
    val perfectDaysTowardNextLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val supabaseUserId: String? = null,
    val leaderboardOptIn: Boolean = false,
    val leaderboardDisplayName: String? = null,
)

data class Streak(
    val id: Long = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: LocalDate? = null,
)

data class DifficultySettings(
    val id: Long = 1,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.MEDIUM,
    val perfectDayThreshold: Float = 0.7f,
    val daysRequiredPerLevel: Int = 7,
)

data class LevelHistory(
    val id: Long = 0,
    val level: Int,
    val achievedDate: Long = System.currentTimeMillis(),
    val difficultyAtTimeOfLevelUp: String,
)

data class TaskCompletionLog(
    val id: Long = 0,
    val taskId: Long,
    val completedDate: LocalDate,
    val wasCompleted: Boolean,
    val pointsAwarded: Int,
)

data class LeaderboardProfile(
    val id: String,
    val displayName: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val level: Int = 1,
    val totalXp: Int = 0,
    val leaderboardOptIn: Boolean = false,
    val updatedAt: String? = null,
)

data class ProfileReport(
    val reporterId: String,
    val reportedProfileId: String,
    val reason: String,
)
