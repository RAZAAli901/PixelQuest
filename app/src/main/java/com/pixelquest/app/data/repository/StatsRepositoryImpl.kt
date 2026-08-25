package com.pixelquest.app.data.repository

import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StatsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val taskCompletionRepository: TaskCompletionRepository,
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val taskRepository: TaskRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository
) : StatsRepository {

    override fun getCompletionRateOverRange(startDate: LocalDate, endDate: LocalDate): Flow<Float> {
        return combine(
            taskRepository.getAllTasks(),
            taskCompletionRepository.getAllLogs()
        ) { tasks, logs ->
            var totalScheduled = 0
            var totalCompleted = 0
            
            val logsByDate = logs.groupBy { it.completedDate }

            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val scheduledTasksForDay = tasks.filter { isTaskScheduledOnDate(it, currentDate) }
                totalScheduled += scheduledTasksForDay.size
                
                val dayLogs = logsByDate[currentDate] ?: emptyList()
                totalCompleted += dayLogs.count { it.wasCompleted }
                
                currentDate = currentDate.plusDays(1)
            }

            if (totalScheduled == 0) 0f else (totalCompleted.toFloat() / totalScheduled.toFloat()).coerceIn(0f, 1f)
        }
    }

    companion object {
        fun isTaskScheduledOnDate(task: com.pixelquest.app.data.local.entity.TaskEntity, date: LocalDate): Boolean {
            if (!task.isActive) return false
            if (date.isBefore(task.scheduledDay)) return false
            return when (task.recurrenceType) {
                com.pixelquest.app.domain.model.RecurrenceType.DAILY -> true
                com.pixelquest.app.domain.model.RecurrenceType.WEEKLY -> date.dayOfWeek == task.scheduledDay.dayOfWeek
                com.pixelquest.app.domain.model.RecurrenceType.MONTHLY -> date.dayOfMonth == task.scheduledDay.dayOfMonth
                com.pixelquest.app.domain.model.RecurrenceType.ONE_TIME -> date == task.scheduledDay
            }
        }
    }

    override fun getDailyStatusForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Map<LocalDate, DailyStatus>> {
        return flowOf(emptyMap())
    }

    override fun getPerTaskStats(taskId: Long): Flow<PerTaskStats> {
        return flowOf(
            PerTaskStats(
                taskId = taskId,
                completionCount = 0,
                totalScheduledCount = 0,
                completionRate = 0f,
                currentStreak = 0,
                longestStreak = 0
            )
        )
    }
}
