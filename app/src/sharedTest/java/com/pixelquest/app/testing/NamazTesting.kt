package com.pixelquest.app.testing

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.AutoMarkMissedUseCase
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.MarkPrayerUseCase
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazPlaceResolver
import com.pixelquest.app.domain.namaz.NamazReminderOptions
import com.pixelquest.app.domain.namaz.NamazReminderOptionsStore
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.domain.namaz.UndoPrayerUseCase
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.namaz.GeoPoint
import com.pixelquest.app.namaz.LocationProvider
import com.pixelquest.app.namaz.NamazAlarms
import com.pixelquest.app.namaz.NamazDeliveries
import com.pixelquest.app.namaz.NamazNotifier
import com.pixelquest.app.namaz.NamazReminder
import com.pixelquest.app.ui.screens.namaz.NamazViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Lahore's time zone, where the Namaz tests' times are worked out. */
val LahoreZone: ZoneId = ZoneId.of("Asia/Karachi")

/** The phone's position for tests: [point], or none (no permission, or no fix). */
class FakeLocationProvider(var point: GeoPoint? = null) : LocationProvider {
    override fun hasPermission(): Boolean = point != null
    override suspend fun currentLocation(): GeoPoint? = point
}

/**
 * A NamazViewModel on fakes (or a real [repository]): the tracker on by default, a [clock] that stands
 * still (16:00 on 17 October 2026 by default, after Asr in Lahore), and [zone].
 */
fun namazViewModel(
    repository: NamazRepository = FakeNamazRepository(NamazSettingsEntity(enabled = true)),
    clock: FixedClock = FixedClock(LocalDateTime.of(2026, 10, 17, 16, 0)),
    zone: ZoneId = LahoreZone,
    location: FakeLocationProvider = FakeLocationProvider(),
    settings: SettingsRepository = FakeSettingsRepository(),
    reminderOptions: NamazReminderOptionsStore = FakeNamazReminderOptionsStore()
): NamazViewModel {
    val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { zone }
    val nextPrayer = NextPrayerUseCase(provider)
    return NamazViewModel(
        repository, provider, nextPrayer,
        AutoMarkMissedUseCase(provider, repository, nextPrayer),
        MarkPrayerUseCase(provider, repository, nextPrayer),
        UndoPrayerUseCase(provider, repository, nextPrayer),
        NamazPlaceResolver(repository, location), settings, reminderOptions, clock
    ) { zone }
}

/** Keeps the reminders "on screen" in a list instead of posting them. */
class FakeNamazNotifier : NamazNotifier {
    val shown = mutableListOf<NamazReminder>()

    /** How often everything was cleared (the tracker turned off). */
    var clearedAll = 0

    override fun show(reminder: NamazReminder) {
        shown.removeAll { it.date == reminder.date && it.prayer == reminder.prayer }
        shown += reminder
    }

    override fun cancel(date: LocalDate, prayer: Prayer) {
        shown.removeAll { it.date == date && it.prayer == prayer }
    }

    override fun clearAll() {
        shown.clear()
        clearedAll++
    }
}

/** The armed alarms in a map, keyed like Android's PendingIntents: one per kind, day and prayer. */
class FakeNamazAlarms(var exact: Boolean = true) : NamazAlarms {
    val armed = linkedMapOf<Triple<NamazAlarm.Kind, LocalDate, Prayer>, NamazAlarm>()

    override fun canScheduleExactAlarms(): Boolean = exact

    override fun schedule(alarm: NamazAlarm) {
        armed[Triple(alarm.kind, alarm.date, alarm.prayer)] = alarm
    }

    override fun cancel(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer) {
        armed.remove(Triple(kind, date, prayer))
    }

    override fun cancelAll(today: LocalDate) {
        armed.keys.removeAll { (_, date, _) -> date in today.minusDays(2)..today.plusDays(3) }
    }
}

/** The reminder options in memory. */
class FakeNamazReminderOptionsStore(initial: NamazReminderOptions = NamazReminderOptions()) : NamazReminderOptionsStore {
    override val options = MutableStateFlow(initial)

    override suspend fun get(): NamazReminderOptions = options.value

    override suspend fun update(change: (NamazReminderOptions) -> NamazReminderOptions) {
        options.value = change(options.value)
    }
}

/** The reminders that have fired, in memory. */
class FakeNamazDeliveries : NamazDeliveries {
    val delivered = mutableSetOf<Triple<NamazAlarm.Kind, LocalDate, Prayer>>()

    override fun wasDelivered(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer): Boolean = Triple(kind, date, prayer) in delivered

    override fun markDelivered(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer) {
        delivered += Triple(kind, date, prayer)
    }
}
