package com.pixelquest.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Posts one quiet AI Coach notification when the insight cooldown ends, so the user knows a
 * fresh insight can be generated. Only while AI Coach and notifications are both on.
 */
@HiltWorker
class InsightReadyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val aiAccess: com.pixelquest.app.domain.ai.AiAccess? = null
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // A fresh insight is only for a signed-in player; signed out, the card asks them to sign in.
        if (aiAccess != null && !aiAccess.isSignedIn.first()) return Result.success()
        if (!settingsRepository.aiInsightsEnabled.first()) return Result.success()
        if (!settingsRepository.isNotificationsEnabled.first()) return Result.success()
        val isSimpleMode = settingsRepository.simpleModeEnabled.first()

        val openApp = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (title, text) = copy(isSimpleMode)
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.Spec.COACH.id)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NotificationHelper.NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 900_002
        private const val WORK_NAME = "InsightReadyNotification"

        fun copy(isSimpleMode: Boolean): Pair<String, String> = if (isSimpleMode) {
            "A new habit insight is available" to "Open PixelQuest to see fresh suggestions based on your recent routine."
        } else {
            "✨ The Questmaster has fresh intel" to "Open PixelQuest for a new debrief on your recent quests."
        }

        /** Replaces any earlier schedule, so only the latest cooldown counts. */
        fun scheduleAfter(context: Context, delayMillis: Long) {
            val request = OneTimeWorkRequestBuilder<InsightReadyWorker>()
                .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
