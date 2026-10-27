package com.pixelquest.app.study.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.data.local.StudyDao
import com.pixelquest.app.study.data.toDomain
import com.pixelquest.app.study.domain.StudyItemKind
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Delivers alarm-style 24-hour due alerts for study items:
 * - High-importance notification channel with alarm sound and vibration.
 * - Category ALARM.
 * - Full-screen intent when device is locked/screen off via StudyPromptActivity.
 * - Actions: Done, Snooze 1 hour, Open.
 */
@AndroidEntryPoint
class StudyDueAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var studyDao: StudyDao

    @Inject
    lateinit var partyDao: com.pixelquest.app.party.data.local.PartyDao

    @Inject
    lateinit var studyRepository: StudyRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var dueAlarmScheduler: StudyDueAlarmScheduler

    @Inject
    lateinit var account: DaroodAccount

    @Inject
    lateinit var reminderMutes: StudyReminderMutes

    var ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == null) return
        handleReceive(context, intent, goAsync())
    }

    internal fun handleReceive(
        context: Context,
        intent: Intent,
        pendingResult: PendingResult? = null,
    ): kotlinx.coroutines.Job {
        val action = intent.action ?: run {
            pendingResult?.finish()
            return kotlinx.coroutines.CompletableDeferred(Unit)
        }
        val itemId = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID) ?: run {
            pendingResult?.finish()
            return kotlinx.coroutines.CompletableDeferred(Unit)
        }
        val partyId = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID).orEmpty()
        val title = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_TITLE).orEmpty()
        val kindStr = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_KIND).orEmpty()
        val dueAtMillis = intent.getLongExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, 0L)

        return CoroutineScope(ioDispatcher).launch {
            try {
                when (action) {
                    StudyDueAlarmScheduler.ACTION_DUE_ALARM -> {
                        handleDueAlarm(context, itemId, partyId, title, kindStr, dueAtMillis)
                    }
                    ACTION_DONE -> {
                        handleDoneAction(context, itemId)
                    }
                    ACTION_SNOOZE -> {
                        handleSnoozeAction(context, itemId, partyId, title, kindStr, dueAtMillis)
                    }
                }
            } catch (t: Throwable) {
                com.pixelquest.app.util.AppLog.e("StudyDueAlarmReceiver", "Failed handling study alarm broadcast", t)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private suspend fun handleDueAlarm(
        context: Context,
        itemId: String,
        partyId: String,
        title: String,
        kindStr: String,
        dueAtMillis: Long,
    ) {
        // Suppress if notifications are globally disabled
        if (!settingsRepository.isNotificationsEnabled.first()) return

        // Suppress if party is muted
        if (!reminderMutes.alerts(partyId)) return

        // Suppress if item is already completed by this user or deleted
        val me = account.id()
        if (me != null && studyDao.isItemCompleted(itemId, me)) return
        val storedItem = studyDao.getItem(itemId) ?: return

        val kind = StudyItemKind.entries.firstOrNull { it.name == kindStr } ?: storedItem.kind
        val dueAt = if (dueAtMillis > 0) Instant.ofEpochMilli(dueAtMillis) else storedItem.dueAt?.let(Instant::ofEpochMilli)

        val notificationTitle = StudyPromptActivity.formatDuePromptTitle(context, kind, dueAt, storedItem.title)
        val partyName = if (partyId.isNotEmpty()) partyDao.getParty(partyId)?.name else null
        val notificationText = if (!partyName.isNullOrBlank()) {
            context.getString(R.string.study_due_alert_prompt_body_party, partyName)
        } else {
            context.getString(R.string.study_due_alert_prompt_body)
        }
        val notificationId = dueAlarmScheduler.requestCodeForItem(itemId)

        // 1. Full-screen prompt intent for lock screen / screen off
        val promptIntent = Intent(context, StudyPromptActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID, partyId)
            putExtra(StudyDueAlarmScheduler.EXTRA_TITLE, storedItem.title)
            putExtra(StudyDueAlarmScheduler.EXTRA_KIND, kind.name)
            putExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, dueAtMillis)
        }
        val promptPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            promptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 2. Open action
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_STUDY_PARTY_ID", partyId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 3. Done action
        val doneIntent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            this.action = ACTION_DONE
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, itemId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 4. Snooze action
        val snoozeIntent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID, partyId)
            putExtra(StudyDueAlarmScheduler.EXTRA_TITLE, storedItem.title)
            putExtra(StudyDueAlarmScheduler.EXTRA_KIND, kind.name)
            putExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, dueAtMillis)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val isAlarmStyle = settingsRepository.isStudyAlarmStyleEnabled.firstOrNull() ?: true
        val alarmSound = settingsRepository.studyAlarmSound.firstOrNull() ?: "alarm"
        val channelId = if (isAlarmStyle) {
            NotificationChannels.Spec.STUDY_DUE_ALARMS.id
        } else {
            NotificationChannels.Spec.STUDY.id
        }

        val builder = PqNotifications.builder(
            context,
            channelId,
            mood = com.pixelquest.app.mascot.PixMood.Focus,
        )
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(if (isAlarmStyle) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, context.getString(R.string.action_done), donePendingIntent)
            .addAction(0, context.getString(R.string.action_snooze_1h), snoozePendingIntent)
            .addAction(0, context.getString(R.string.action_open), openPendingIntent)

        if (isAlarmStyle) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setFullScreenIntent(promptPendingIntent, true)
            val soundUri = when (alarmSound) {
                "chime" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                "system" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                else -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            }
            if (soundUri != null) {
                builder.setSound(soundUri)
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Missing notification permission on Android 13+
        }
    }

    private suspend fun handleDoneAction(context: Context, itemId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(dueAlarmScheduler.requestCodeForItem(itemId))

        val item = studyDao.getItem(itemId)?.toDomain() ?: return
        studyRepository.complete(item)
    }

    private fun handleSnoozeAction(
        context: Context,
        itemId: String,
        partyId: String,
        title: String,
        kindStr: String,
        dueAtMillis: Long,
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(dueAlarmScheduler.requestCodeForItem(itemId))

        val snoozeTrigger = System.currentTimeMillis() + SNOOZE_DURATION_MS
        val intent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = StudyDueAlarmScheduler.ACTION_DUE_ALARM
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID, partyId)
            putExtra(StudyDueAlarmScheduler.EXTRA_TITLE, title)
            putExtra(StudyDueAlarmScheduler.EXTRA_KIND, kindStr)
            putExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, dueAtMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            dueAlarmScheduler.requestCodeForItem(itemId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
        alarmManager?.let {
            com.pixelquest.app.scheduling.AlarmArming.arm(it, snoozeTrigger, pendingIntent)
        }
    }

    companion object {
        const val ACTION_DONE = "com.pixelquest.app.action.STUDY_DUE_DONE"
        const val ACTION_SNOOZE = "com.pixelquest.app.action.STUDY_DUE_SNOOZE"
        const val SNOOZE_DURATION_MS = 60 * 60 * 1000L // 1 hour
    }
}
