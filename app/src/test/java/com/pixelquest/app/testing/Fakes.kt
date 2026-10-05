package com.pixelquest.app.testing

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelCalculator
import com.pixelquest.app.domain.TaskOccurrence
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.LevelHistoryRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/*
 * In-memory repositories for unit tests. They follow the real implementations' rules (a task's
 * occurrences, one completion log per task and day, level-up progress) so tests exercise app
 * behaviour rather than a stub's. Tests seed state through the public MutableStateFlows.
 */

class FakeTaskRepository(initial: List<TaskEntity> = emptyList()) : TaskRepository {
    val tasks = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun getAllTasks(): Flow<List<TaskEntity>> = tasks
    override fun getTaskById(id: Long): Flow<TaskEntity?> = tasks.map { all -> all.firstOrNull { it.id == id } }

    /** Same rule as TaskRepositoryImpl: active tasks that occur on [day]. */
    override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> = tasks.map { all ->
        all.filter {
            it.isActive && !it.scheduledDay.isAfter(day) &&
                TaskOccurrence.occursOn(it.scheduledDay, it.recurrenceType, day, it.weeklyDays)
        }.sortedBy { it.scheduledTime }
    }

    override suspend fun insertTask(task: TaskEntity): Long {
        val stored = if (task.id == 0L) task.copy(id = nextId++) else task.also { nextId = maxOf(nextId, it.id + 1) }
        tasks.value = tasks.value.filterNot { it.id == stored.id } + stored
        return stored.id
    }
    override suspend fun updateTask(task: TaskEntity) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }
    override suspend fun deleteTask(task: TaskEntity) {
        tasks.value = tasks.value.filterNot { it.id == task.id }
    }
}

/** Like the real table, holds at most one log per task per day (a second insert returns -1). */
class FakeTaskCompletionRepository(initial: List<TaskCompletionLogEntity> = emptyList()) : TaskCompletionRepository {
    val logs = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun insertLog(log: TaskCompletionLogEntity): Long {
        if (logs.value.any { it.taskId == log.taskId && it.completedDate == log.completedDate }) return -1
        val stored = log.copy(id = nextId++)
        logs.value = logs.value + stored
        return stored.id
    }
    override suspend fun updateLog(log: TaskCompletionLogEntity) {
        logs.value = logs.value.map { if (it.id == log.id) log else it }
    }
    override suspend fun getLogForTaskOnDate(taskId: Long, date: LocalDate) =
        logs.value.firstOrNull { it.taskId == taskId && it.completedDate == date }
    override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> =
        logs.map { all -> all.filter { it.completedDate == date } }
    override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> =
        logs.map { all -> all.filter { it.taskId == taskId }.sortedByDescending { it.completedDate } }
    override fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>> =
        logs.map { all -> all.filter { it.completedDate in startDate..endDate }.sortedBy { it.completedDate } }
    override fun getAllLogs(): Flow<List<TaskCompletionLogEntity>> = logs.map { all -> all.sortedBy { it.completedDate } }
}

class FakeUserProfileRepository(
    initial: UserProfileEntity? = UserProfileEntity(username = "Hero", avatarId = "avatar_hero")
) : UserProfileRepository {
    val profile = MutableStateFlow(initial)

    override fun getProfile(): Flow<UserProfileEntity?> = profile
    override suspend fun insertProfile(profile: UserProfileEntity) { this.profile.value = profile }
    override suspend fun updateProfile(profile: UserProfileEntity) { this.profile.value = profile }
    override suspend fun performLevelUp(): UserProfileEntity? {
        val current = profile.value ?: return null
        return current.copy(level = current.level + 1, perfectDaysTowardNextLevel = LevelCalculator.getPostLevelUpProgress())
            .also { profile.value = it }
    }
    override suspend fun updateSupabaseUserId(userId: String?) {
        profile.value = profile.value?.copy(supabaseUserId = userId)
    }
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
        profile.value = profile.value?.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
    }
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
        profile.value = profile.value?.copy(leaderboardOptIn = optIn)
    }
    override suspend fun clearCloudData() {
        profile.value = profile.value?.copy(supabaseUserId = null, leaderboardOptIn = false, leaderboardDisplayName = null)
    }
}

class FakeStreakRepository(initial: StreakEntity? = StreakEntity()) : StreakRepository {
    val streak = MutableStateFlow(initial)
    override fun getCurrentStreak(): Flow<StreakEntity?> = streak
    override suspend fun insertStreak(streak: StreakEntity) { this.streak.value = streak }
    override suspend fun updateStreak(streak: StreakEntity) { this.streak.value = streak }
}

class FakeDifficultySettingsRepository(initial: DifficultySettingsEntity? = DifficultySettingsEntity()) : DifficultySettingsRepository {
    val settings = MutableStateFlow(initial)
    override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> = settings
    override suspend fun insertSettings(settings: DifficultySettingsEntity) { this.settings.value = settings }
    override suspend fun updateSettings(settings: DifficultySettingsEntity) { this.settings.value = settings }
}

class FakeLevelHistoryRepository : LevelHistoryRepository {
    val history = MutableStateFlow<List<LevelHistoryEntity>>(emptyList())
    override fun getAllHistory(): Flow<List<LevelHistoryEntity>> = history
    override suspend fun insertLevelHistory(entry: LevelHistoryEntity) { history.value = history.value + entry }
}

/** Every setting as a MutableStateFlow; the setters write through, like SharedPreferences would. */
class FakeSettingsRepository(
    simpleMode: Boolean = false,
    theme: ThemeMode = ThemeMode.Pixel,
    notifications: Boolean = true,
    aiInsights: Boolean = false,
    onboardingDone: Boolean = true
) : SettingsRepository {
    override val isSoundEnabled = MutableStateFlow(true)
    override val isCrtEnabled = MutableStateFlow(false)
    override val isHapticsEnabled = MutableStateFlow(true)
    override val isReduceMotionEnabled = MutableStateFlow(false)
    override val onboardingComplete = MutableStateFlow(onboardingDone)
    override val isNotificationsEnabled = MutableStateFlow(notifications)
    override val isNotificationSoundEnabled = MutableStateFlow(true)
    override val isNotificationVibrationEnabled = MutableStateFlow(true)
    override val themeMode = MutableStateFlow(theme)
    override val simpleModeEnabled = MutableStateFlow(simpleMode)
    override val hasSeenSimpleModeHighlight = MutableStateFlow(false)
    override val hasSeenComicModeHighlight = MutableStateFlow(false)
    override val aiInsightsEnabled = MutableStateFlow(aiInsights)
    override val lastAiInsightTimestamp = MutableStateFlow(0L)
    override val aiReminderMessagesEnabled = MutableStateFlow(false)
    var themeBeforeComic: ThemeMode = ThemeMode.Pixel

    override suspend fun setSimpleModeHighlightSeen(seen: Boolean) { hasSeenSimpleModeHighlight.value = seen }
    override suspend fun setComicModeHighlightSeen(seen: Boolean) { hasSeenComicModeHighlight.value = seen }
    override suspend fun setSoundEnabled(enabled: Boolean) { isSoundEnabled.value = enabled }
    override suspend fun setCrtEnabled(enabled: Boolean) { isCrtEnabled.value = enabled }
    override suspend fun setHapticsEnabled(enabled: Boolean) { isHapticsEnabled.value = enabled }
    override suspend fun setReduceMotionEnabled(enabled: Boolean) { isReduceMotionEnabled.value = enabled }
    override suspend fun setOnboardingComplete(complete: Boolean) { onboardingComplete.value = complete }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { isNotificationsEnabled.value = enabled }
    override suspend fun setNotificationSoundEnabled(enabled: Boolean) { isNotificationSoundEnabled.value = enabled }
    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) { isNotificationVibrationEnabled.value = enabled }
    override suspend fun setThemeMode(mode: ThemeMode) {
        if (mode == ThemeMode.Comic && themeMode.value != ThemeMode.Comic) themeBeforeComic = themeMode.value
        themeMode.value = mode
    }
    override suspend fun getThemeModeBeforeComic(): ThemeMode = themeBeforeComic
    override suspend fun setSimpleModeEnabled(enabled: Boolean) { simpleModeEnabled.value = enabled }
    override suspend fun setAiInsightsEnabled(enabled: Boolean) { aiInsightsEnabled.value = enabled }
    override suspend fun setLastAiInsightTimestamp(timestamp: Long) { lastAiInsightTimestamp.value = timestamp }
    override suspend fun setAiReminderMessagesEnabled(enabled: Boolean) { aiReminderMessagesEnabled.value = enabled }
}
