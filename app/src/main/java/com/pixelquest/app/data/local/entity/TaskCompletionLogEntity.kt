package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "task_completion_logs")
data class TaskCompletionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val completedDate: LocalDate,
    val wasCompleted: Boolean,
    val pointsAwarded: Int
)
