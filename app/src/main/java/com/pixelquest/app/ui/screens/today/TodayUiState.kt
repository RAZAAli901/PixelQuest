package com.pixelquest.app.ui.screens.today

import androidx.compose.runtime.Immutable
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.ui.components.TaskItemStatus
import java.time.LocalTime

@Immutable
data class TodayTaskItem(
    val task: TaskEntity,
    val status: TaskItemStatus,
    val scheduledTime: LocalTime = task.scheduledTime,
    /** The XP its completion earned today; 0 until it's done. */
    val xp: Int = 0,
)

@Immutable
sealed class TodayUiState {
    @Immutable
    object Loading : TodayUiState()

    @Immutable
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
        val flavorText: String = "",
        val isSimpleMode: Boolean = false,
        /** The player's name and avatar, for Home's header. */
        val heroName: String = "",
        val avatarId: String = "",
        /** The XP today's completed quests earned. */
        val xpToday: Int = 0,
    ) : TodayUiState()

    @Immutable
    data class Error(val message: String) : TodayUiState()
}
