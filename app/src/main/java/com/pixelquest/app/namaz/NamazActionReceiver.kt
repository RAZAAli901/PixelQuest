package com.pixelquest.app.namaz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelquest.app.domain.namaz.MarkPrayerUseCase
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.util.AppClock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
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
class NamazActionHandler(
    private val notifier: NamazNotifier,
    private val markPrayer: MarkPrayerUseCase,
    private val alarms: NamazAlarms,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) {
    @Inject
    constructor(notifier: NamazNotifier, markPrayer: MarkPrayerUseCase, alarms: NamazAlarms, clock: AppClock) :
        this(notifier, markPrayer, alarms, clock, { ZoneId.systemDefault() })

    suspend fun handle(action: NamazAction, date: LocalDate, prayer: Prayer) {
        val now = clock.now().atZone(zone())
        when (action) {
            // Marked as the screen's button marks it, so the same rules apply, and from Phase 6
            // the same XP. A prayer whose time hasn't come isn't marked (MarkPrayerUseCase).
            NamazAction.PRAYED -> markPrayer(date, prayer, now)
            // The reminder again in 10 minutes: its own alarm, beside the prayer's regular ones.
            // Tapping Snooze again moves it, rather than adding another.
            NamazAction.SNOOZE -> alarms.schedule(
                NamazAlarm(NamazAlarm.Kind.SNOOZE, date, prayer, now.plusMinutes(SNOOZE_MINUTES).toInstant().toEpochMilli())
            )
            NamazAction.DISMISS -> Unit
        }
        notifier.cancel(date, prayer)
    }

    companion object {
        const val SNOOZE_MINUTES = 10L
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
                com.pixelquest.app.util.AppLog.e("NamazActionReceiver", "Could not handle $action for $prayer on $date", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
