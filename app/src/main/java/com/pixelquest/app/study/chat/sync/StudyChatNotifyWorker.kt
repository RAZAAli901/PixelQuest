package com.pixelquest.app.study.chat.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.party.data.local.PartyDao
import com.pixelquest.app.study.chat.data.StudyChatRepository
import com.pixelquest.app.study.chat.data.local.ChatDao
import com.pixelquest.app.ui.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Which parties' chats notify on this phone: all, unless the player mutes one. */
interface ChatMutes {
    fun muted(partyId: String): Boolean
    fun setMuted(partyId: String, muted: Boolean)
    val mutedFlow: kotlinx.coroutines.flow.StateFlow<Set<String>>
}

@Singleton
class PrefsChatMutes @Inject constructor(@ApplicationContext context: Context) : ChatMutes {
    private val prefs = context.getSharedPreferences("pixelquest_study_chat", Context.MODE_PRIVATE)
    private val state = kotlinx.coroutines.flow.MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    override val mutedFlow: kotlinx.coroutines.flow.StateFlow<Set<String>> = state
    override fun muted(partyId: String) = partyId in state.value

    @Synchronized
    override fun setMuted(partyId: String, muted: Boolean) {
        val next = if (muted) state.value + partyId else state.value - partyId
        prefs.edit().putStringSet(KEY, next).apply()
        state.value = next
    }

    private companion object {
        const val KEY = "muted_chats"
    }
}

/**
 * New messages and files in the player's study chats (docs/study-chat.md, Notifications): every 30
 * minutes with a connection, each active study party's chat is brought up to date, and one that
 * has others' messages the player hasn't read (and hasn't been told of) gets one notification, on
 * the Study chat channel, its text hidden on the lock screen. A muted party, or the app's own
 * notifications off, gets none.
 */
@HiltWorker
class StudyChatNotifyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val chat: StudyChatRepository,
    private val dao: ChatDao,
    private val parties: PartyDao,
    private val mutes: ChatMutes,
    private val settings: com.pixelquest.app.domain.repository.SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!settings.isNotificationsEnabled.first()) return Result.success()
        val prefs = applicationContext.getSharedPreferences("pixelquest_study_chat", Context.MODE_PRIVATE)
        val study = parties.observeParties().first().filter { it.partyType.name == "STUDY" && it.myStatus.name == "ACTIVE" }
        for (party in study) {
            chat.refresh(party.id)
        }
        val unread = chat.observeUnread().first()
        for (party in study) {
            val count = unread[party.id] ?: 0
            val newest = dao.newestMessage(party.id)?.createdAt ?: continue
            val told = prefs.getLong("told_${party.id}", 0L)
            if (count == 0 || newest <= told || mutes.muted(party.id)) continue
            notify(party.id, party.name, count)
            prefs.edit().putLong("told_${party.id}", newest).apply()
        }
        return Result.success()
    }

    private fun notify(partyId: String, partyName: String, count: Int) {
        val context = applicationContext
        val open = PendingIntent.getActivity(
            context, partyId.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_ROUTE, Screen.PartiesParty.createRoute(partyId))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannels.Spec.STUDY_CHAT.id
        val hidden = PqNotifications.builder(context, channel).setContentTitle(context.getString(R.string.study_chat_notif_hidden)).build()
        val notification = PqNotifications.builder(context, channel)
            .setContentTitle(partyName)
            .setContentText(context.resources.getQuantityString(R.plurals.study_chat_notif_new, count, count))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(hidden)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(930_000 + (partyId.hashCode() and 0xFFFF), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }

    companion object {
        private const val WORK = "pixelquest_study_chat_notify"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StudyChatNotifyWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
