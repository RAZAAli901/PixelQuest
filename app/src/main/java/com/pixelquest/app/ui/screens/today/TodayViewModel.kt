package com.pixelquest.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.FlavorTextCatalog
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val taskResultRecorder: TaskResultRecorder,
    private val syncScheduler: com.pixelquest.app.worker.SyncScheduler? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val refreshRequests = MutableStateFlow(0)

    /** Emits now, then at the start of every minute, while the screen is collecting. */
    private val minuteTicks = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            delay(Duration.between(now, now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)).toMillis())
        }
    }

    /**
     * The time the screen is drawn for. It moves on every minute and on [refresh], so the list
     * switches to the new day at midnight even if the app stayed open, and pending quests turn
     * into grace-period ones as their time passes.
     */
    private val clock: StateFlow<LocalDateTime> = combine(minuteTicks, refreshRequests) { _, _ -> LocalDateTime.now() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    /** The day the screen shows, which completing and skipping record against. */
    private val currentDate: LocalDate get() = clock.value.toLocalDate()

    private val _quickCompleteFlourishEvent = MutableStateFlow<Boolean?>(null)
    val quickCompleteFlourishEvent: StateFlow<Boolean?> = _quickCompleteFlourishEvent.asStateFlow()

    fun dismissFlourishEvent() {
        _quickCompleteFlourishEvent.value = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dayData = clock.map { it.toLocalDate() }.distinctUntilChanged().flatMapLatest { date ->
        combine(
            taskRepository.getTasksForDay(date),
            taskCompletionRepository.getLogsForDate(date),
            streakRepository.getCurrentStreak()
        ) { tasks, logs, streak -> DayData(date, tasks, logs, streak) }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        dayData,
        combine(
            userProfileRepository.getProfile(),
            difficultySettingsRepository.getCurrentDifficulty(),
            settingsRepository?.simpleModeEnabled ?: flowOf(false)
        ) { profile, difficulty, simpleMode -> Triple(profile, difficulty, simpleMode) },
        clock
    ) { (date, tasks, logs, streak), (profile, difficulty, isSimpleMode), now ->
        val logMap = logs.associateBy { it.taskId }
        val items = tasks.map { task ->
            val log = logMap[task.id]
            // A day still on screen after midnight (until the next tick) counts as fully past.
            val nowTime = if (now.toLocalDate() == date) now.toLocalTime() else LocalTime.MAX
            val status = when {
                log?.wasCompleted == true -> TaskItemStatus.DONE
                log?.wasCompleted == false -> TaskItemStatus.MISSED
                nowTime.isAfter(task.scheduledTime) -> TaskItemStatus.GRACE_PERIOD
                else -> TaskItemStatus.PENDING
            }
            TodayTaskItem(
                task = task,
                status = status,
                scheduledTime = task.scheduledTime
            )
        }
        val sortedItems = items.sortedWith(
            compareBy<TodayTaskItem> { it.status == TaskItemStatus.DONE || it.status == TaskItemStatus.MISSED }
                .thenBy { it.scheduledTime }
        )

        val completedCount = sortedItems.count { it.status == TaskItemStatus.DONE }
        val totalCount = sortedItems.size
        val completionPct = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
        val threshold = difficulty?.perfectDayThreshold ?: 0.7f
        val isPerfectDay = totalCount > 0 && completionPct >= threshold
        val flavorText = FlavorTextCatalog.getFlavorText(
            taskCount = totalCount,
            completedCount = completedCount,
            isPerfectDay = isPerfectDay,
            date = date,
            isSimpleMode = isSimpleMode
        )

        val isStreakBroken = (streak != null && streak.currentStreak == 0 && streak.lastCompletedDate != null)

        TodayUiState.Success(
            tasks = sortedItems,
            currentStreak = streak?.currentStreak ?: 0,
            totalXp = profile?.totalXp ?: 0,
            level = profile?.level ?: 1,
            perfectDaysTowardNextLevel = profile?.perfectDaysTowardNextLevel ?: 0,
            daysRequiredPerLevel = difficulty?.daysRequiredPerLevel ?: 7,
            completionPercentage = completionPct,
            targetThreshold = threshold,
            isPerfectDay = isPerfectDay,
            isStreakBroken = isStreakBroken,
            flavorText = flavorText,
            isSimpleMode = isSimpleMode
        ) as TodayUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState.Loading
    )

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            // 0 means it was already done (a double tap, or completed from the reminder).
            if (taskResultRecorder.recordCompleted(task.id, currentDate) == 0) return@launch
            rearmAfterToday(task)
            taskAlarmScheduler.clearReminder(task.id)
            val isSimple = settingsRepository?.simpleModeEnabled?.first() ?: false
            _quickCompleteFlourishEvent.value = !isSimple
            syncScheduler?.scheduleProfileSync()
        }
    }

    /**
     * Today's reminder is no longer needed, but a recurring task still needs tomorrow's.
     * Re-arming replaces today's pending alarm (same request code); one-time tasks are cancelled.
     */
    private fun rearmAfterToday(task: TaskEntity) {
        if (task.recurrenceType == RecurrenceType.ONE_TIME) {
            taskAlarmScheduler.cancelAlarmForTask(task)
        } else {
            taskAlarmScheduler.scheduleNextOccurrence(task)
        }
    }

    /** Re-reads the clock: the REFRESH button, and returning to the app, can move to a new day. */
    fun refresh() {
        refreshRequests.value++
    }

    fun skipTask(task: TaskEntity) {
        viewModelScope.launch {
            if (taskResultRecorder.recordNotDone(task.id, currentDate)) {
                rearmAfterToday(task)
                taskAlarmScheduler.clearReminder(task.id)
            }
        }
    }
}

private data class DayData(
    val date: LocalDate,
    val tasks: List<TaskEntity>,
    val logs: List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>,
    val streak: StreakEntity?
)
