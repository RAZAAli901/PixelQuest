package com.pixelquest.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.worker.MissedTaskWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PixelQuestApplication : Application(), Configuration.Provider {

    // Without this factory WorkManager can't build @HiltWorker classes (they need injected repositories),
    // and every background worker failed with NoSuchMethodException.
    @javax.inject.Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @javax.inject.Inject
    lateinit var connectivitySyncObserver: com.pixelquest.app.worker.ConnectivitySyncObserver

    @javax.inject.Inject
    lateinit var taskRepository: com.pixelquest.app.domain.repository.TaskRepository

    @javax.inject.Inject
    lateinit var settingsRepository: com.pixelquest.app.domain.repository.SettingsRepository

    @javax.inject.Inject
    lateinit var taskAlarmScheduler: com.pixelquest.app.scheduling.TaskAlarmScheduler

    @javax.inject.Inject
    lateinit var soundManager: com.pixelquest.app.audio.SoundManager

    private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Step 8: Global uncaught-exception handler logging crashes locally
        com.pixelquest.app.util.PixelCrashHandler.init(this)
        // Fast cold-start: lightweight notification channel creation
        NotificationHelper.createNotificationChannel(this)
        // SFX and haptics settings apply everywhere, including the full-screen prompt.
        com.pixelquest.app.audio.FeedbackSettingsSync.start(appScope, settingsRepository, soundManager)
        // Start connectivity observer for prompt sync retry on reconnection
        connectivitySyncObserver.startObserving()
        // Background async enqueue of periodic background workers
        scheduleMissedTaskWorker()
        scheduleStreakEvaluationWorker()
        // Evening streak-at-risk nudge; posts nothing unless a live streak is actually at risk.
        com.pixelquest.app.worker.StreakAtRiskWorker.schedule(this)
        // Does nothing unless AI Coach and AI reminder messages are both on.
        com.pixelquest.app.worker.EncouragementPackWorker.schedule(this)
        rearmReminders()
    }

    /**
     * Re-arms every task's next reminder on launch. Each alarm replaces the task's existing one, so
     * this is safe to repeat, and it repairs installs where earlier versions never armed alarms.
     */
    private fun rearmReminders() {
        appScope.launch {
            try {
                if (!settingsRepository.isNotificationsEnabled.first()) return@launch
                taskAlarmScheduler.rescheduleAllAlarms(taskRepository.getAllTasks().first().filter { it.isActive })
            } catch (e: Exception) {
                android.util.Log.w("PixelQuestApplication", "Could not re-arm reminders on launch", e)
            }
        }
    }

    private fun scheduleMissedTaskWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MissedTaskWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MissedTaskWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleStreakEvaluationWorker() {
        val now = java.time.LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(5)
        val initialDelayMinutes = java.time.Duration.between(now, nextMidnight).toMinutes()

        val workRequest = PeriodicWorkRequestBuilder<com.pixelquest.app.worker.StreakEvaluationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StreakEvaluationWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

