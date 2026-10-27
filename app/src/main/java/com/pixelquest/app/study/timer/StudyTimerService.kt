package com.pixelquest.app.study.timer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.mascot.PixMood
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.study.data.RunningTimer
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.data.StudyTimerStore
import com.pixelquest.app.study.domain.StudyRules
import com.pixelquest.app.study.ui.elapsedText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that owns the study timer state machine (RUNNING, PAUSED, STOPPED).
 * Maintains an ongoing notification with live chronometer, action buttons (Pause/Resume/Stop),
 * and automatic 4-hour max study duration enforcement.
 */
@AndroidEntryPoint
class StudyTimerService : Service() {

    @Inject
    lateinit var timerStore: StudyTimerStore

    @Inject
    lateinit var studyRepository: StudyRepository

    @Inject
    lateinit var partyDao: com.pixelquest.app.party.data.local.PartyDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var autoStopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        observeTimerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val partyId = intent.getStringExtra(EXTRA_PARTY_ID) ?: return START_NOT_STICKY
                val partyName = intent.getStringExtra(EXTRA_PARTY_NAME)
                val startedAt = intent.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis())
                handleStart(partyId, partyName, startedAt)
            }
            ACTION_PAUSE -> {
                handlePause()
            }
            ACTION_RESUME -> {
                handleResume()
            }
            ACTION_STOP -> {
                handleStop()
            }
            ACTION_CANCEL -> {
                handleCancel()
            }
            ACTION_RESTORE -> {
                handleRestore()
            }
        }

        return START_STICKY
    }

    private fun observeTimerState() {
        serviceScope.launch {
            timerStore.running.collectLatest { timer ->
                if (timer == null) {
                    autoStopJob?.cancel()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    updateNotification(timer)
                    scheduleAutoStopIfNeeded(timer)
                }
            }
        }
    }

    private fun handleStart(partyId: String, partyName: String?, startedAt: Long) {
        serviceScope.launch(Dispatchers.IO) {
            val current = timerStore.running.value
            if (current != null && current.partyId != partyId) {
                performStop()
            }
            val resolvedName = partyName ?: partyDao.getParty(partyId)?.name
            timerStore.start(partyId, startedAt, resolvedName)
        }
    }

    private fun handlePause() {
        timerStore.pause(System.currentTimeMillis())
    }

    private fun handleResume() {
        timerStore.resume(System.currentTimeMillis())
    }

    internal suspend fun performStop(now: Long = System.currentTimeMillis()): Int? {
        val current = timerStore.running.value ?: return null
        val loggedMinutes = current.loggedAt(now)

        if (loggedMinutes != null) {
            studyRepository.logStudy(current.partyId, loggedMinutes)
            showCompletionNotification(
                getString(R.string.study_timer_saved_notif, loggedMinutes),
            )
        } else {
            showCompletionNotification(
                getString(R.string.study_timer_not_saved),
            )
        }
        timerStore.clear()
        return loggedMinutes
    }

    private fun handleStop() {
        serviceScope.launch(Dispatchers.IO) {
            performStop()
        }
    }

    private fun handleCancel() {
        timerStore.clear()
    }

    private fun handleRestore() {
        val current = timerStore.running.value
        if (current == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            // Check if 4 hours already elapsed while process was killed
            val now = System.currentTimeMillis()
            val elapsed = current.elapsedMillisAt(now)
            if (elapsed >= MAX_STUDY_MILLIS) {
                handleStop()
            } else {
                updateNotification(current)
                scheduleAutoStopIfNeeded(current)
            }
        }
    }

    private fun scheduleAutoStopIfNeeded(timer: RunningTimer) {
        autoStopJob?.cancel()
        if (timer.isPaused) return

        val now = System.currentTimeMillis()
        val elapsed = timer.elapsedMillisAt(now)
        val remaining = (MAX_STUDY_MILLIS - elapsed).coerceAtLeast(0L)

        if (remaining <= 0L) {
            handleStop()
            return
        }

        autoStopJob = serviceScope.launch {
            delay(remaining)
            handleStop()
        }
    }

    private fun updateNotification(timer: RunningTimer) {
        val notification = buildForegroundNotification(timer)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        }
    }

    internal fun buildForegroundNotification(timer: RunningTimer): Notification {
        val partyName = timer.partyName
        val title = if (!partyName.isNullOrBlank()) {
            getString(R.string.study_timer_notif_title, partyName)
        } else {
            getString(R.string.study_timer_notif_title_solo)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, com.pixelquest.app.ui.navigation.Screen.PartiesParty.createRoute(timer.partyId))
            putExtra("OPEN_STUDY_PARTY_ID", timer.partyId)
            putExtra("FOCUS_TAB", "WEEK")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = NotificationChannels.Spec.STUDY_TIMER.id
        val builder = PqNotifications.builder(this, channelId, mood = PixMood.Focus)
            .setContentTitle(title)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val stopIntent = Intent(this, StudyTimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (timer.isPaused) {
            val elapsed = timer.elapsedMillisAt(System.currentTimeMillis())
            builder.setContentText(getString(R.string.study_timer_notif_paused, elapsedText(elapsed)))
            builder.setUsesChronometer(false)

            val resumeIntent = Intent(this, StudyTimerService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = PendingIntent.getService(
                this,
                NOTIFICATION_ID + 2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, getString(R.string.study_timer_action_resume), resumePendingIntent)
        } else {
            val base = timer.chronometerBase(SystemClock.elapsedRealtime(), System.currentTimeMillis())
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(false)
            builder.setWhen(base)

            val pauseIntent = Intent(this, StudyTimerService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(
                this,
                NOTIFICATION_ID + 3,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, getString(R.string.study_timer_action_pause), pausePendingIntent)
        }

        builder.addAction(0, getString(R.string.study_timer_action_stop), stopPendingIntent)

        return builder.build()
    }

    private fun showCompletionNotification(message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = NotificationChannels.Spec.STUDY.id
        val notification = PqNotifications.builder(this, channelId, mood = PixMood.Focus)
            .setContentTitle(getString(R.string.study_timer_title))
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        manager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.pixelquest.app.study.timer.ACTION_START"
        const val ACTION_PAUSE = "com.pixelquest.app.study.timer.ACTION_PAUSE"
        const val ACTION_RESUME = "com.pixelquest.app.study.timer.ACTION_RESUME"
        const val ACTION_STOP = "com.pixelquest.app.study.timer.ACTION_STOP"
        const val ACTION_CANCEL = "com.pixelquest.app.study.timer.ACTION_CANCEL"
        const val ACTION_RESTORE = "com.pixelquest.app.study.timer.ACTION_RESTORE"

        const val EXTRA_PARTY_ID = "party_id"
        const val EXTRA_PARTY_NAME = "party_name"
        const val EXTRA_STARTED_AT = "started_at"

        const val NOTIFICATION_ID = 4001
        const val COMPLETION_NOTIFICATION_ID = 4002
        val MAX_STUDY_MILLIS = StudyRules.MINUTES_MAX * 60_000L // 240 minutes (4 hours)

        fun start(context: Context, partyId: String, partyName: String? = null, startedAt: Long = System.currentTimeMillis()) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PARTY_ID, partyId)
                putExtra(EXTRA_PARTY_NAME, partyName)
                putExtra(EXTRA_STARTED_AT, startedAt)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply { action = ACTION_PAUSE }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply { action = ACTION_RESUME }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply { action = ACTION_CANCEL }
            context.startService(intent)
        }

        fun restore(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply { action = ACTION_RESTORE }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hasNotificationPermission(context: Context): Boolean = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
