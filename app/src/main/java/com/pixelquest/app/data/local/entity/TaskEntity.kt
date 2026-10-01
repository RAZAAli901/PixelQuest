package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val scheduledDay: LocalDate,
    val scheduledTime: LocalTime,
    val recurrenceType: RecurrenceType,
    val category: TaskCategory,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // Day 26: per-task reminder settings (MIGRATION_4_5 adds these with the defaults below)
    val reminderEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 0,
    val reminderStyle: ReminderStyle = ReminderStyle.STANDARD
)
