package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.data.local.prefs.EncouragementPack
import com.pixelquest.app.data.local.prefs.EncouragementPackStore
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiException
import com.pixelquest.app.domain.ai.AiUsageTracker
import com.pixelquest.app.domain.ai.EncouragementPackInput
import com.pixelquest.app.domain.ai.EncouragementPackPrompt
import com.pixelquest.app.domain.ai.EncouragementSanitizer
import com.pixelquest.app.domain.ai.HabitInsightTone
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Fetches the day's AI-written reminder lines: at most one Gemini call per day, only while both
 * AI Coach and AI reminder messages are on, and counted against the shared daily/monthly caps.
 * Failures are not retried; reminders simply use the built-in lines.
 */
@HiltWorker
class EncouragementPackWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val geminiClient: GeminiClient,
    private val aiUsageTracker: AiUsageTracker,
    private val packStore: EncouragementPackStore,
    private val streakRepository: StreakRepository,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!settingsRepository.aiInsightsEnabled.first()) return Result.success()
        if (!settingsRepository.aiReminderMessagesEnabled.first()) return Result.success()

        val today = LocalDate.now()
        val isSimpleMode = settingsRepository.simpleModeEnabled.first()
        val tone = if (isSimpleMode) HabitInsightTone.SIMPLE_MINIMALIST else HabitInsightTone.GAMIFIED_HEROIC
        val existing = packStore.load()
        if (existing != null && existing.generatedOn == today && existing.tone == tone) return Result.success()
        if (!aiUsageTracker.canMakeCall(today)) return Result.success()

        val input = EncouragementPackInput(
            currentStreak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0,
            completionRatePercent7d = completionRateLast7Days(today),
            activeTaskCount = taskRepository.getAllTasks().first().count { it.isActive },
            tone = tone
        )

        aiUsageTracker.recordCall(today)
        val raw = try {
            geminiClient.generateContent(
                prompt = EncouragementPackPrompt.prompt(input),
                systemInstruction = EncouragementPackPrompt.systemInstruction(tone)
            )
        } catch (e: GeminiException) {
            return Result.success()
        }

        val lines = EncouragementSanitizer.clean(EncouragementPackPrompt.parse(raw), isSimpleMode)
        if (lines.size >= MIN_USABLE_LINES) {
            packStore.save(EncouragementPack(generatedOn = today, tone = tone, messages = lines))
        }
        return Result.success()
    }

    /** Completed share of the tasks that were due over the previous seven days. */
    private suspend fun completionRateLast7Days(today: LocalDate): Int {
        var due = 0
        var done = 0
        for (offset in 1..7L) {
            val day = today.minusDays(offset)
            val dueIds = taskRepository.getTasksForDay(day).first().map { it.id }.toSet()
            due += dueIds.size
            done += taskCompletionRepository.getLogsForDate(day).first()
                .count { it.wasCompleted && it.taskId in dueIds }
        }
        return if (due == 0) 0 else (done * 100 / due)
    }

    companion object {
        private const val MIN_USABLE_LINES = 2
        private const val PERIODIC_NAME = "EncouragementPackDaily"
        private const val NOW_NAME = "EncouragementPackNow"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EncouragementPackWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Used right after the user turns AI reminder messages on. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<EncouragementPackWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOW_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
