package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One result per task per day: completed (with the XP it earned) or not (missed or skipped).
 * The unique index (Day 28, MIGRATION_6_7) stops a task being counted twice on the same day.
 */
@Entity(
    tableName = "task_completion_logs",
    indices = [Index(value = ["taskId", "completedDate"], unique = true)]
)
data class TaskCompletionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val completedDate: LocalDate,
    val wasCompleted: Boolean,
    val pointsAwarded: Int
)
