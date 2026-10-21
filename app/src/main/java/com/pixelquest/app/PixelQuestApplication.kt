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

    @javax.inject.Inject
    lateinit var aiAccess: dagger.Lazy<com.pixelquest.app.domain.ai.AiAccess>

    @javax.inject.Inject
    lateinit var encouragementPackStore: com.pixelquest.app.data.local.prefs.EncouragementPackStore

    @javax.inject.Inject
    lateinit var namazReminders: com.pixelquest.app.namaz.NamazReminders

    @javax.inject.Inject
    lateinit var daroodSync: com.pixelquest.app.darood.sync.DaroodSyncScheduler

    @javax.inject.Inject
    lateinit var studySync: com.pixelquest.app.study.sync.StudySyncScheduler

    @javax.inject.Inject
    lateinit var studyReminders: com.pixelquest.app.study.data.StudyReminderSettings

    @javax.inject.Inject
    lateinit var daroodCleanup: dagger.Lazy<com.pixelquest.app.darood.sync.DaroodAccountCleanup>

    @javax.inject.Inject
    lateinit var studyReminderAlarms: com.pixelquest.app.study.alarm.StudyReminderAlarmSync

    private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Step 8: Global uncaught-exception handler logging crashes locally
        com.pixelquest.app.util.PixelCrashHandler.init(this)
        // Fast cold-start: lightweight notification channel creation
        NotificationHelper.createNotificationChannel(this)
        // SFX and haptics settings apply everywhere, including the full-screen prompt.
        com.pixelquest.app.audio.FeedbackSettingsSync.start(appScope, settingsRepository, soundManager)
        // Signed out, saved AI-written reminder lines are dropped (the AI Coach is for signed-in players).
        com.pixelquest.app.auth.AiSignOutCleanup.start(appScope, { aiAccess.get() }, encouragementPackStore)
        // Darood Party's data is the signed-in account's: cleared on sign-out or a switch of account.
        appScope.launch { daroodCleanup.get().start(this) }
        // Start connectivity observer for prompt sync retry on reconnection
        connectivitySyncObserver.startObserving()
        // The next 48 hours of prayer reminders, armed again whenever the tracker's settings change,
        // and once a day as a safety net.
        namazReminders.start(appScope)
        // Study reminders' alarms, armed again whenever the phone's reminders change (docs/study-reminders.md).
        studyReminderAlarms.start(appScope)
        // The settings file, loaded off the main thread so MainActivity's first read finds it ready.
        appScope.launch { com.pixelquest.app.data.repository.SettingsRepositoryImpl.warm(this@PixelQuestApplication) }
        // The background work's schedules: none of it has to be in place before the first frame,
        // so it's queued off the main thread, which then only starts the app.
        appScope.launch {
            // Background async enqueue of periodic background workers
            scheduleMissedTaskWorker()
            // Nightly at 02:05 local time; replaces the old 24-hour periodic job (see StreakEvaluationWorker.RUN_AT).
            com.pixelquest.app.worker.StreakEvaluationWorker.schedule(this@PixelQuestApplication)
            // Evening streak-at-risk nudge; posts nothing unless a live streak is actually at risk.
            com.pixelquest.app.worker.StreakAtRiskWorker.schedule(this@PixelQuestApplication)
            // A daily backup kept on the phone (AutoBackups), restorable from Settings, and one an hour
            // after the app is opened, so it holds that session's play.
            com.pixelquest.app.worker.AutoBackupWorker.schedule(this@PixelQuestApplication)
            com.pixelquest.app.worker.AutoBackupWorker.scheduleAfterPlay(this@PixelQuestApplication)
            // Does nothing unless AI Coach and AI reminder messages are both on.
            com.pixelquest.app.worker.EncouragementPackWorker.schedule(this@PixelQuestApplication)
            // Namaz Tracker: each day's five pending rows just after midnight, and hourly missed marks.
            // Both do nothing while the tracker is off.
            com.pixelquest.app.worker.NamazMidnightWorker.schedule(this@PixelQuestApplication)
            com.pixelquest.app.worker.NamazMissedWorker.schedule(this@PixelQuestApplication)
            com.pixelquest.app.worker.NamazRescheduleWorker.schedule(this@PixelQuestApplication)
            // Darood entries left waiting when the app last closed go once there's a connection.
            daroodSync.scheduleSend()
            // So do study party ticks and study time (docs/study-party.md).
            studySync.scheduleSend()
            // Reminders of what's due, unless the player turned them off.
            if (studyReminders.dueReminders.value) com.pixelquest.app.study.sync.StudyDueWorker.schedule(this@PixelQuestApplication)
            // The Quran reader's text, loaded from the app's own files on the first start (docs/quran-data.md).
            com.pixelquest.app.quran.data.QuranImportWorker.schedule(this@PixelQuestApplication)
        }
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
}

