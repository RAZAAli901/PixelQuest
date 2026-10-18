package com.pixelquest.app.namaz

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazReminderPlanner
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The scheduler's times, window and cancelling, on the planner and Android's AlarmManager:
 * each alarm exactly at its prayer's start (less the offset), only inside the next 48 hours, right
 * across a clock change, and all of them cancelled together.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazSchedulingTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val scheduler = NamazAlarmScheduler(context)
    private val day = LocalDate.of(2026, 10, 17)

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }

    private fun planner(zone: ZoneId, settings: NamazSettingsEntity = NamazSettingsEntity(enabled = true)): Pair<NamazReminderPlanner, DefaultPrayerTimeProvider> {
        val repository = FakeNamazRepository(settings)
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), FixedClock()) { zone }
        return NamazReminderPlanner(provider) to provider
    }

    @Test
    fun everyAlarm_isAtItsPrayersStart_toTheMillisecond() = runBlocking {
        val (planner, provider) = planner(LahoreZone)
        val now = LocalDateTime.of(day, LocalTime.of(0, 30)).atZone(LahoreZone)

        val alarms = planner.plan(NamazSettingsEntity(enabled = true), now)

        alarms.forEach { alarm ->
            assertEquals("${alarm.prayer} on ${alarm.date}", provider.timesFor(alarm.date).timeOf(alarm.prayer).toInstant().toEpochMilli(), alarm.atMillis)
        }
        // From 00:30: two full days of five.
        assertEquals(10, alarms.size)
    }

    @Test
    fun theWindow_isTheNext48Hours_exactly() = runBlocking {
        val (planner, provider) = planner(LahoreZone)
        val maghrib = provider.timesFor(day).maghrib

        // At Maghrib itself: it's not planned (it's now), but two days on it's exactly 48 hours ahead.
        val atMaghrib = planner.plan(NamazSettingsEntity(enabled = true), maghrib)
        assertTrue(atMaghrib.none { it.prayer == Prayer.MAGHRIB && it.date == day })
        assertTrue(atMaghrib.all { Duration.between(maghrib.toInstant(), Instant.ofEpochMilli(it.atMillis)) <= Duration.ofHours(48) })
        assertTrue(atMaghrib.all { Instant.ofEpochMilli(it.atMillis).isAfter(maghrib.toInstant()) })

        // A second before Maghrib, that day's Maghrib is the first alarm.
        val justBefore = planner.plan(NamazSettingsEntity(enabled = true), maghrib.minusSeconds(1))
        assertEquals(Prayer.MAGHRIB to day, justBefore.first().let { it.prayer to it.date })
    }

    @Test
    fun acrossTheClocksGoingForward_eachAlarmIsStillAtItsRealTime() = runBlocking {
        // New York, the night the clocks went forward (8 March 2026, 02:00).
        val newYork = ZoneId.of("America/New_York")
        val (planner, provider) = planner(newYork, NamazSettingsEntity(enabled = true, latitude = 40.7128, longitude = -74.0060, method = "NORTH_AMERICA"))
        val now = LocalDateTime.of(2026, 3, 7, 22, 0).atZone(newYork)

        val alarms = planner.plan(NamazSettingsEntity(enabled = true, latitude = 40.7128, longitude = -74.0060, method = "NORTH_AMERICA"), now)

        alarms.forEach { alarm ->
            assertEquals(provider.timesFor(alarm.date).timeOf(alarm.prayer).toInstant().toEpochMilli(), alarm.atMillis)
        }
        // Dhuhr on the 8th is at its daylight-time clock time, about 13:0x, not an hour off.
        val dhuhr = alarms.first { it.prayer == Prayer.DHUHR && it.date == LocalDate.of(2026, 3, 8) }
        assertEquals(13, Instant.ofEpochMilli(dhuhr.atMillis).atZone(newYork).hour)
    }

    @Test
    fun withoutExactAlarms_eachLandsInATenMinuteWindow() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.REMINDER, day, Prayer.ASR, 5_000L))

        val alarm = alarmManager.scheduledAlarms.single()
        assertEquals(5_000L, alarm.triggerAtTime)
        assertEquals(NamazAlarmScheduler.INEXACT_WINDOW_MS, alarm.windowLengthMs)
    }

    @Test
    fun withExactAlarms_eachIsExact() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.REMINDER, day, Prayer.ASR, 5_000L))

        assertEquals(0L, alarmManager.scheduledAlarms.single().windowLengthMs)
    }

    @Test
    fun cancelAll_reachesEveryKind_inTheWholeWindow() {
        val kinds = NamazAlarm.Kind.entries
        for (offset in -2L..3L) for (kind in kinds) {
            scheduler.schedule(NamazAlarm(kind, day.plusDays(offset), Prayer.entries[(offset + 2).toInt() % 5], 1_000L + offset))
        }
        assertEquals(6 * kinds.size, alarmManager.scheduledAlarms.size)

        scheduler.cancelAll(day)

        assertTrue(alarmManager.scheduledAlarms.isEmpty())
    }
}
