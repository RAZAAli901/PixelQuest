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
        return combine(
            taskRepository.getAllTasks(),
            taskCompletionRepository.getAllLogs(),
            difficultySettingsRepository.getCurrentDifficulty()
        ) { tasks, logs, difficultyEntity ->
            val difficultyLevel = difficultyEntity?.difficultyLevel ?: com.pixelquest.app.domain.model.DifficultyLevel.MEDIUM
            val threshold = com.pixelquest.app.domain.DifficultyMode.getPerfectDayThreshold(difficultyLevel)
            
            val logsByDate = logs.groupBy { it.completedDate }
            val resultMap = mutableMapOf<LocalDate, DailyStatus>()

            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val scheduledTasks = tasks.filter { isTaskScheduledOnDate(it, currentDate) }
                if (scheduledTasks.isEmpty()) {
                    resultMap[currentDate] = DailyStatus.NO_TASKS_SCHEDULED
                } else {
                    val dayLogs = logsByDate[currentDate] ?: emptyList()
                    val completedCount = dayLogs.count { it.wasCompleted }
                    val isPerfect = com.pixelquest.app.domain.StreakCalculator.isPerfectDay(completedCount, scheduledTasks.size, threshold)
                    val status = when {
                        isPerfect -> DailyStatus.PERFECT
                        completedCount > 0 -> DailyStatus.PARTIAL
                        else -> DailyStatus.MISSED
                    }
                    resultMap[currentDate] = status
                }
                currentDate = currentDate.plusDays(1)
            }

            resultMap
        }
    }

    override fun getPerTaskStats(taskId: Long): Flow<PerTaskStats> {
        return combine(
            taskRepository.getTaskById(taskId),
            taskCompletionRepository.getLogsForTask(taskId)
        ) { task, logs ->
            if (task == null) {
                return@combine PerTaskStats(
                    taskId = taskId,
                    completionCount = 0,
                    totalScheduledCount = 0,
                    completionRate = 0f,
                    currentStreak = 0,
                    longestStreak = 0
                )
            }

            val today = LocalDate.now()
            var totalScheduled = 0
            var checkDate = task.scheduledDay
            while (!checkDate.isAfter(today)) {
                if (isTaskScheduledOnDate(task, checkDate)) {
                    totalScheduled++
                }
                checkDate = checkDate.plusDays(1)
            }
            val totalScheduledCount = maxOf(totalScheduled, logs.size)

            val completedLogs = logs.filter { it.wasCompleted }
            val completionCount = completedLogs.size
            val rate = if (totalScheduledCount == 0) 0f else (completionCount.toFloat() / totalScheduledCount.toFloat()).coerceIn(0f, 1f)

            // Calculate streaks from chronological logs
            val sortedLogs = logs.sortedBy { it.completedDate }
            var currentStreak = 0
            var longestStreak = 0
            var tempStreak = 0

            for (log in sortedLogs) {
                if (log.wasCompleted) {
                    tempStreak++
                    if (tempStreak > longestStreak) {
                        longestStreak = tempStreak
                    }
                } else {
                    tempStreak = 0
                }
            }

            // Current streak counts backward from latest log
            for (log in sortedLogs.reversed()) {
                if (log.wasCompleted) {
                    currentStreak++
                } else {
                    break
                }
            }

            val recentHistory = sortedLogs.takeLast(14).map { Pair(it.completedDate, it.wasCompleted) }

            PerTaskStats(
                taskId = taskId,
                completionCount = completionCount,
                totalScheduledCount = totalScheduledCount,
                completionRate = rate,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                recentHistory = recentHistory
            )
        }
    }
}
