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
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val appClock: com.pixelquest.app.util.AppClock = com.pixelquest.app.util.AppClock()
) : StatsRepository {

    override fun getCompletionRateOverRange(startDate: LocalDate, endDate: LocalDate): Flow<Float> {
        if (startDate.isAfter(endDate)) return flowOf(0f)
        return combine(
            taskRepository.getAllTasks(),
            taskCompletionRepository.getAllLogs()
        ) { tasks, logs ->
            if (tasks.isEmpty()) return@combine 0f
            var totalScheduled = 0
            var totalCompleted = 0
            
            val logsByDate = logs.groupBy { it.completedDate }
            val today = appClock.now().toLocalDate()

            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val dayLogs = logsByDate[currentDate] ?: emptyList()
                val scheduledTasksForDay = tasks.filter { isTaskScheduledOnDate(it, currentDate) }.let { due ->
                    // Today isn't over: only quests that already have a result count, as in the AI
                    // Coach's 7-day rate, so the rate doesn't drop every morning.
                    if (currentDate == today) {
                        val logged = dayLogs.map { it.taskId }.toSet()
                        due.filter { it.id in logged }
                    } else due
                }
                totalScheduled += scheduledTasksForDay.size

                totalCompleted += completedScheduledCount(scheduledTasksForDay, dayLogs)
                
                currentDate = currentDate.plusDays(1)
            }

            if (totalScheduled == 0) 0f else (totalCompleted.toFloat() / totalScheduled.toFloat()).coerceIn(0f, 1f)
        }
    }

    companion object {
        /**
         * Completed tasks among those scheduled that day, each counted once. Logs left by deleted
         * or rescheduled tasks don't count, so they can't push a day (or the rate) to 100%.
         */
        fun completedScheduledCount(
            scheduled: List<com.pixelquest.app.data.local.entity.TaskEntity>,
            dayLogs: List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>
        ): Int {
            val scheduledIds = scheduled.map { it.id }.toSet()
            return dayLogs.filter { it.wasCompleted && it.taskId in scheduledIds }.map { it.taskId }.toSet().size
        }

        fun isTaskScheduledOnDate(task: com.pixelquest.app.data.local.entity.TaskEntity, date: LocalDate): Boolean {
            if (!task.isActive) return false
            // Same rule as the Today list and reminders, including a weekly task's chosen days.
            return com.pixelquest.app.domain.TaskOccurrence.occursOn(task.scheduledDay, task.recurrenceType, date, task.weeklyDays)
        }
    }

    override fun getDailyStatusForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Map<LocalDate, DailyStatus>> {
        if (startDate.isAfter(endDate)) return flowOf(emptyMap())
        return combine(
            taskRepository.getAllTasks(),
            taskCompletionRepository.getAllLogs(),
            difficultySettingsRepository.getCurrentDifficulty()
        ) { tasks, logs, difficultyEntity ->
            val difficultyLevel = difficultyEntity?.difficultyLevel ?: com.pixelquest.app.domain.model.DifficultyLevel.MEDIUM
            val threshold = com.pixelquest.app.domain.DifficultyMode.getPerfectDayThreshold(difficultyLevel)
            
            val logsByDate = logs.groupBy { it.completedDate }
            val resultMap = mutableMapOf<LocalDate, DailyStatus>()
            val today = appClock.now().toLocalDate()

            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val scheduledTasks = tasks.filter { isTaskScheduledOnDate(it, currentDate) }
                if (scheduledTasks.isEmpty()) {
                    resultMap[currentDate] = DailyStatus.NO_TASKS_SCHEDULED
                } else {
                    val dayLogs = logsByDate[currentDate] ?: emptyList()
                    val completedCount = completedScheduledCount(scheduledTasks, dayLogs)
                    val isPerfect = com.pixelquest.app.domain.StreakCalculator.isPerfectDay(completedCount, scheduledTasks.size, threshold)
                    val status = when {
                        isPerfect -> DailyStatus.PERFECT
                        completedCount > 0 -> DailyStatus.PARTIAL
                        // Today isn't over: nothing done yet is not a missed day.
                        currentDate == today -> DailyStatus.IN_PROGRESS
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

            val today = appClock.now().toLocalDate()
            val doneDates = logs.filter { it.wasCompleted }.map { it.completedDate }.toSet()
            val loggedDates = logs.map { it.completedDate }.toSet()

            // The quest's occurrences so far: every day it was due before today, and any day with a
            // result (including today once answered, and days from before a schedule change). A past
            // due day with no result counts as not done; today doesn't count until it has a result,
            // so the rate doesn't dip every morning. Whether the quest is still active doesn't matter
            // for its own history.
            val dueDays = generateSequence(task.scheduledDay) { it.plusDays(1) }
                .takeWhile { it.isBefore(today) }
                .filter { com.pixelquest.app.domain.TaskOccurrence.occursOn(task.scheduledDay, task.recurrenceType, it, task.weeklyDays) }
            val occurrences = (dueDays.toSet() + loggedDates.filter { !it.isAfter(today) }).sorted()

            val totalScheduledCount = occurrences.size
            val completionCount = occurrences.count { it in doneDates }
            val rate = if (totalScheduledCount == 0) 0f else (completionCount.toFloat() / totalScheduledCount.toFloat()).coerceIn(0f, 1f)

            // Streaks run over occurrences in order: a done one extends, anything else resets.
            var currentStreak = 0
            var longestStreak = 0
            for (day in occurrences) {
                currentStreak = if (day in doneDates) currentStreak + 1 else 0
                longestStreak = maxOf(longestStreak, currentStreak)
            }

            val recentHistory = occurrences.takeLast(14).map { Pair(it, it in doneDates) }

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
