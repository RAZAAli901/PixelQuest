package com.pixelquest.app.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazReminderOptions
import com.pixelquest.app.domain.namaz.NamazReminderPlanner
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazAlarms
import com.pixelquest.app.testing.FakeNamazReminderOptionsStore
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The 48-hour window, armed and kept in step with the settings (Lahore, 16:00 on 17 October 2026). */
class NamazRemindersTest {

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
    private val options = FakeNamazReminderOptionsStore()
    private val clock = FixedClock(LocalDateTime.of(day, LocalTime.of(16, 0)))
    private val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
    private val planner = NamazReminderPlanner(provider)
    private val alarms = FakeNamazAlarms()
    private val reminders = NamazReminders(repository, options, planner, alarms, clock) { LahoreZone }

    private fun planned() = runBlocking {
        planner.plan(repository.getSettings(), clock.time.atZone(LahoreZone), options.get()).toSet()
    }

    @Test
    fun reschedule_armsExactlyThePlan_andAgainChangesNothing() = runBlocking {
        reminders.reschedule()
        assertEquals(planned(), alarms.armed.values.toSet())
        assertEquals(10, alarms.armed.size)

        reminders.reschedule()
        assertEquals(planned(), alarms.armed.values.toSet())
    }

    @Test
    fun aChangedOffsetOrAPrayerTurnedOff_replacesTheOldAlarms() = runBlocking {
        reminders.reschedule()

        repository.saveSettings(repository.getSettings().copy(reminderOffsetMin = 15, remindIsha = false))
        reminders.reschedule()

        assertEquals(planned(), alarms.armed.values.toSet())
        assertTrue(alarms.armed.values.none { it.prayer == Prayer.ISHA })
    }

    @Test
    fun aPendingSnooze_survivesAReschedule() = runBlocking {
        val snooze = NamazAlarm(NamazAlarm.Kind.SNOOZE, day, Prayer.ASR, 123L)
        alarms.schedule(snooze)

        reminders.reschedule()

        assertTrue(snooze in alarms.armed.values)
    }

    @Test
    fun theTrackerTurnedOff_leavesNoReminderArmed() = runBlocking {
        reminders.reschedule()

        repository.saveSettings(repository.getSettings().copy(enabled = false))
        reminders.reschedule()

        assertTrue(alarms.armed.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun once_started_itFollowsTheSettingsAndOptions() = runTest(UnconfinedTestDispatcher()) {
        reminders.start(backgroundScope)
        assertEquals(10, alarms.armed.size)

        options.update { it.copy(nudge = true) }
        assertTrue(alarms.armed.values.any { it.kind == NamazAlarm.Kind.NUDGE })

        repository.saveSettings(repository.getSettings().copy(remindMaghrib = false))
        assertTrue(alarms.armed.values.none { it.prayer == Prayer.MAGHRIB })
        assertEquals(runBlocking { planned() }, alarms.armed.values.toSet())
    }
}
