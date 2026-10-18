package com.pixelquest.app.namaz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelquest.app.domain.namaz.Prayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A button on a prayer's reminder. */
enum class NamazAction(val action: String, val codeBase: Int) {
    PRAYED("com.pixelquest.app.action.NAMAZ_PRAYED", 750_000),
    SNOOZE("com.pixelquest.app.action.NAMAZ_SNOOZE_TAPPED", 760_000),
    DISMISS("com.pixelquest.app.action.NAMAZ_DISMISS", 770_000);

    /** Its own PendingIntent per prayer and day, like the alarms (NamazAlarm.requestCode). */
    fun requestCode(date: LocalDate, prayer: Prayer): Int =
        codeBase + Math.floorMod(date.toEpochDay(), 100L).toInt() * 10 + prayer.ordinal

    companion object {
        fun fromAction(action: String?): NamazAction? = entries.firstOrNull { it.action == action }
    }
}

/** What a reminder's buttons do; the receiver only hands them over. */
@Singleton
class NamazActionHandler @Inject constructor(
    private val notifier: NamazNotifier
) {
    suspend fun handle(action: NamazAction, date: LocalDate, prayer: Prayer) {
        when (action) {
            NamazAction.PRAYED, NamazAction.SNOOZE, NamazAction.DISMISS -> Unit
        }
        notifier.cancel(date, prayer)
    }
}

/** Receives a reminder's buttons (manifest, not exported). */
@AndroidEntryPoint
class NamazActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handler: NamazActionHandler

    override fun onReceive(context: Context, intent: Intent) {
        val action = NamazAction.fromAction(intent.action) ?: return
        val date = NamazAlarmScheduler.dateFrom(intent) ?: return
        val prayer = NamazAlarmScheduler.prayerFrom(intent) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.handle(action, date, prayer)
            } catch (e: Exception) {
                android.util.Log.e("NamazActionReceiver", "Could not handle $action for $prayer on $date", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
