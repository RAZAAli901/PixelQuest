package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Instant

/** Which alarms are armed: the next 48 hours of enabled prayers, at their start less the offset. */
class NamazReminderPlannerTest {

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository()
    private val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), FixedClock()) { LahoreZone }
    private val planner = NamazReminderPlanner(provider)
    private val now = LocalDateTime.of(day, LocalTime.of(16, 0)).atZone(LahoreZone)

    private fun plan(settings: NamazSettingsEntity) = runBlocking { planner.plan(settings, now) }

    @Test
    fun theNext48Hours_ofEveryPrayer_atItsStart() = runBlocking {
        val alarms = plan(NamazSettingsEntity(enabled = true))

        // From 16:00 on the 17th: Maghrib and Isha that day, all five on the 18th, and Fajr to Asr on the 19th.
        assertEquals(10, alarms.size)
        assertEquals(listOf(Prayer.MAGHRIB, Prayer.ISHA), alarms.take(2).map { it.prayer })
        assertTrue(alarms.all { it.kind == NamazAlarm.Kind.REMINDER })
        assertTrue(alarms.all { Instant.ofEpochMilli(it.atMillis).isAfter(now.toInstant()) })
        assertTrue(alarms.all { Duration.between(now.toInstant(), Instant.ofEpochMilli(it.atMillis)) <= Duration.ofHours(48) })
        val maghrib = alarms.first()
        assertEquals(day, maghrib.date)
        assertEquals(provider.timesFor(day).maghrib.toInstant().toEpochMilli(), maghrib.atMillis)
    }

    @Test
    fun theOffset_bringsEachReminderForward() {
        val alarms = plan(NamazSettingsEntity(enabled = true, reminderOffsetMin = 10))

        val maghrib = alarms.first { it.prayer == Prayer.MAGHRIB && it.date == day }
        assertEquals(runBlocking { provider.timesFor(day).maghrib }.minusMinutes(10).toInstant().toEpochMilli(), maghrib.atMillis)
    }

    @Test
    fun onlyPrayersWhoseReminderIsOn() {
        val alarms = plan(NamazSettingsEntity(enabled = true, remindFajr = false, remindIsha = false))

        assertTrue(alarms.none { it.prayer == Prayer.FAJR || it.prayer == Prayer.ISHA })
        assertTrue(alarms.isNotEmpty())
    }

    @Test
    fun nothing_whileTheTrackerIsOff() {
        assertTrue(plan(NamazSettingsEntity(enabled = false)).isEmpty())
    }

    @Test
    fun afterMidnight_yesterdaysIshaIsPast_butTodaysFajrIsPlanned() = runBlocking {
        val late = LocalDateTime.of(day.plusDays(1), LocalTime.of(1, 0)).atZone(LahoreZone)
        val alarms = planner.plan(NamazSettingsEntity(enabled = true), late)

        assertEquals(PrayerMoment(Prayer.FAJR, day.plusDays(1), provider.timesFor(day.plusDays(1)).fajr).let { it.prayer to it.day },
            alarms.first().let { it.prayer to it.date })
    }

    @Test
    fun withTheNudgeOn_eachPrayerAlsoGetsOne_20MinutesBeforeTheNext() = runBlocking {
        val alarms = planner.plan(NamazSettingsEntity(enabled = true), now, NamazReminderOptions(nudge = true))
        val nudges = alarms.filter { it.kind == NamazAlarm.Kind.NUDGE }

        // Asr's nudge comes before Maghrib; Isha's before the next day's Fajr.
        val asr = nudges.first { it.prayer == Prayer.ASR && it.date == day }
        assertEquals(provider.timesFor(day).maghrib.minusMinutes(20).toInstant().toEpochMilli(), asr.atMillis)
        val isha = nudges.first { it.prayer == Prayer.ISHA && it.date == day }
        assertEquals(provider.timesFor(day.plusDays(1)).fajr.minusMinutes(20).toInstant().toEpochMilli(), isha.atMillis)
        assertTrue(plan(NamazSettingsEntity(enabled = true)).none { it.kind == NamazAlarm.Kind.NUDGE })
    }

    @Test
    fun fajrThroughDoNotDisturb_armsOnlyFajrsReminder_asAnAlarmClock() = runBlocking {
        val alarms = planner.plan(NamazSettingsEntity(enabled = true), now, NamazReminderOptions(fajrThroughDnd = true, nudge = true))

        assertTrue(alarms.filter { it.prayer == Prayer.FAJR && it.kind == NamazAlarm.Kind.REMINDER }.all { it.alarmClock })
        assertTrue(alarms.filterNot { it.prayer == Prayer.FAJR && it.kind == NamazAlarm.Kind.REMINDER }.none { it.alarmClock })
        // Silent reminders have nothing to let through.
        val silent = planner.plan(NamazSettingsEntity(enabled = true), now, NamazReminderOptions(fajrThroughDnd = true, sound = NamazSound.SILENT))
        assertTrue(silent.none { it.alarmClock })
    }
}
