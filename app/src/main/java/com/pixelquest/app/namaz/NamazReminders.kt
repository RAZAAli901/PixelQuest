package com.pixelquest.app.namaz

import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazReminderOptionsStore
import com.pixelquest.app.domain.namaz.NamazReminderPlanner
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.util.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the Namaz alarms armed for the next 48 hours (docs/namaz-design.md, Reminders):
 * [reschedule] arms NamazReminderPlanner's list, and [start] reschedules whenever the tracker's
 * settings, place or reminder options change. The day's change and each alarm firing reschedule too.
 * Settings' NOTIFICATIONS: OFF silences the whole app, so then nothing is armed. With the tracker
 * turned off, everything goes: every alarm (snoozes too), its notifications, and its channels.
 */
@Singleton
class NamazReminders(
    private val repository: NamazRepository,
    private val options: NamazReminderOptionsStore,
    private val planner: NamazReminderPlanner,
    private val alarms: NamazAlarms,
    private val notifier: NamazNotifier,
    private val deliveries: NamazDeliveries,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) {
    @Inject
    constructor(
        repository: NamazRepository,
        options: NamazReminderOptionsStore,
        planner: NamazReminderPlanner,
        alarms: NamazAlarms,
        notifier: NamazNotifier,
        deliveries: NamazDeliveries,
        settingsRepository: SettingsRepository,
        clock: AppClock
    ) : this(repository, options, planner, alarms, notifier, deliveries, settingsRepository, clock, { ZoneId.systemDefault() })

    private val mutex = Mutex()

    /**
     * Arms the planned reminders and nudges, and cancels any armed one that's no longer planned (a
     * prayer turned off, an offset changed). The same alarm armed again replaces itself, so this
     * can run as often as it likes. A snooze or test reminder is left as it is.
     */
    suspend fun reschedule(): Unit = mutex.withLock {
        val now = clock.now().atZone(zone())
        val settings = repository.getSettings()
        if (!settings.enabled) {
            alarms.cancelAll(now.toLocalDate())
            notifier.clearAll()
            return@withLock
        }
        val plan = if (settingsRepository.isNotificationsEnabled.first()) {
            // Never one that has already fired: a later time for it (a changed offset, a new time
            // zone) would remind the same prayer twice.
            planner.plan(settings, now, options.get()).filterNot { deliveries.wasDelivered(it.kind, it.date, it.prayer) }
        } else {
            emptyList()
        }
        val planned = plan.map { Triple(it.kind, it.date, it.prayer) }.toSet()
        for (offset in -2L..3L) {
            val date = now.toLocalDate().plusDays(offset)
            for (kind in RESCHEDULED_KINDS) for (prayer in Prayer.entries) {
                if (Triple(kind, date, prayer) !in planned) alarms.cancel(kind, date, prayer)
            }
        }
        plan.forEach { alarms.schedule(it) }
    }

    /** Reschedules now, and again each time the tracker's settings, reminder options or notifications switch change. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(repository.observeSettings(), options.options, settingsRepository.isNotificationsEnabled) { settings, opts, on ->
                Triple(settings, opts, on)
            }
                .distinctUntilChanged()
                .collectLatest {
                    try {
                        reschedule()
                    } catch (e: Exception) {
                        android.util.Log.w("NamazReminders", "Could not reschedule the prayer reminders", e)
                    }
                }
        }
    }

    private companion object {
        /** The kinds the plan decides; snoozes and test reminders are one-offs. */
        val RESCHEDULED_KINDS = listOf(NamazAlarm.Kind.REMINDER, NamazAlarm.Kind.NUDGE)
    }
}
