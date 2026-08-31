package com.pixelquest.app.ui.screens.today

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.ui.components.TaskItemStatus
import java.time.LocalTime

data class TodayTaskItem(
    val task: TaskEntity,
    val status: TaskItemStatus,
    val scheduledTime: LocalTime = task.scheduledTime
)

sealed class TodayUiState {
    object Loading : TodayUiState()
    data class Success(
        val tasks: List<TodayTaskItem> = emptyList(),
        val currentStreak: Int = 0,
        val totalXp: Int = 0,
        val level: Int = 1,
        val perfectDaysTowardNextLevel: Int = 0,
        val daysRequiredPerLevel: Int = 7,
        val completionPercentage: Float = 0f,
        val targetThreshold: Float = 0.7f,
        val isPerfectDay: Boolean = false,
        val isStreakBroken: Boolean = false,
        val flavorText: String = ""
    ) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
}
