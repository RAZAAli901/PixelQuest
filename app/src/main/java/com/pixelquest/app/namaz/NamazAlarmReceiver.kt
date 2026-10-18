package com.pixelquest.app.namaz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazReminderCheck
import com.pixelquest.app.domain.namaz.NamazReminderOptionsStore
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerMoment
import com.pixelquest.app.domain.namaz.PrayerTimeProvider
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.util.AppClock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** What a Namaz alarm does when it fires; the receiver only hands it over. */
@Singleton
class NamazAlarmHandler(
    private val repository: NamazRepository,
    private val provider: PrayerTimeProvider,
    private val notifier: NamazNotifier,
    private val options: NamazReminderOptionsStore,
    private val deliveries: NamazDeliveries,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) {
    @Inject
    constructor(
        repository: NamazRepository,
        provider: PrayerTimeProvider,
        notifier: NamazNotifier,
        options: NamazReminderOptionsStore,
        deliveries: NamazDeliveries,
        clock: AppClock
    ) : this(repository, provider, notifier, options, deliveries, clock, { ZoneId.systemDefault() })

    /** Shows the reminder for [prayer] on [date], unless it's no longer wanted (NamazReminderCheck). */
    suspend fun handle(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer) {
        // Fired, whether or not it shows: it's never armed again (NamazReminders).
        if (kind == NamazAlarm.Kind.REMINDER || kind == NamazAlarm.Kind.NUDGE) deliveries.markDelivered(kind, date, prayer)
        val settings = repository.getSettings()
        if (!NamazReminderCheck.shouldRemind(kind, prayer, settings, repository.getDay(date))) return
        val place = DefaultPrayerTimeProvider.placeOf(settings.latitude, settings.longitude, settings.city)
        val times = provider.timesFor(date)
        val time = times.timeOf(prayer)
        // A reminder set to come early: whole minutes still to go, rounded up.
        val secondsToStart = Duration.between(clock.now().atZone(zone()), time).seconds
        val minutesToStart = if (secondsToStart <= 0) 0 else ((secondsToStart + 59) / 60).toInt()
        // The nudge says when the next prayer starts (after Isha, tomorrow's Fajr).
        val next = if (kind == NamazAlarm.Kind.NUDGE) {
            prayer.nextInDay?.let { PrayerMoment(it, date, times.timeOf(it)) }
                ?: PrayerMoment(Prayer.FAJR, date.plusDays(1), provider.timesFor(date.plusDays(1)).fajr)
        } else {
            null
        }
        val opts = options.get()
        notifier.show(
            NamazReminder(
                kind, date, prayer, time, place.name, minutesToStart, next, opts.sound,
                // The nudge is a quiet follow-up, never an alarm.
                asAlarm = kind != NamazAlarm.Kind.NUDGE && opts.asAlarm(prayer)
            )
        )
    }
}

/**
 * Receives the Namaz alarms (manifest, not exported). Reads which alarm it is and hands it to
 * [NamazAlarmHandler] off the main thread. A reminder or nudge firing then rolls the 48-hour
 * window on, so there's always the next one armed.
 */
@AndroidEntryPoint
class NamazAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handler: NamazAlarmHandler

    @Inject
    lateinit var reminders: NamazReminders

    override fun onReceive(context: Context, intent: Intent) {
        val kind = NamazAlarm.Kind.fromAction(intent.action) ?: return
        val date = NamazAlarmScheduler.dateFrom(intent) ?: return
        val prayer = NamazAlarmScheduler.prayerFrom(intent) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.handle(kind, date, prayer)
                if (kind == NamazAlarm.Kind.REMINDER || kind == NamazAlarm.Kind.NUDGE) reminders.reschedule()
            } catch (e: Exception) {
                android.util.Log.e("NamazAlarmReceiver", "Could not show the $kind for $prayer on $date", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
