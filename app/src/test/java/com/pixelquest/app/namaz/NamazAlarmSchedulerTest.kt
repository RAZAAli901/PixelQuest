package com.pixelquest.app.namaz

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Arming and cancelling Namaz alarms: one per prayer and kind, each with its own identity. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazAlarmSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val scheduler = NamazAlarmScheduler(context)
    private val day = LocalDate.of(2026, 10, 17)

    private fun reminder(prayer: Prayer, at: Long = 1_000_000L, date: LocalDate = day) =
        NamazAlarm(NamazAlarm.Kind.REMINDER, date, prayer, at)

    @Test
    fun eachPrayer_getsItsOwnAlarm_atItsTime() {
        Prayer.entries.forEachIndexed { i, prayer -> scheduler.schedule(reminder(prayer, at = 1_000_000L + i)) }

        assertEquals(5, alarms.scheduledAlarms.size)
        assertEquals((0..4).map { 1_000_000L + it }.toSet(), alarms.scheduledAlarms.map { it.triggerAtTime }.toSet())
        assertTrue(alarms.scheduledAlarms.all { it.type == AlarmManager.RTC_WAKEUP })
    }

    @Test
    fun armingTheSameAlarmAgain_replacesIt() {
        scheduler.schedule(reminder(Prayer.ASR, at = 1_000L))
        scheduler.schedule(reminder(Prayer.ASR, at = 2_000L))

        assertEquals(1, alarms.scheduledAlarms.size)
        assertEquals(2_000L, alarms.scheduledAlarms.single().triggerAtTime)
    }

    @Test
    fun kindsAndDays_dontReplaceEachOther() {
        scheduler.schedule(reminder(Prayer.ASR))
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.NUDGE, day, Prayer.ASR, 1_000_000L))
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.SNOOZE, day, Prayer.ASR, 1_000_000L))
        scheduler.schedule(reminder(Prayer.ASR, date = day.plusDays(1)))

        assertEquals(4, alarms.scheduledAlarms.size)
    }

    @Test
    fun cancelling_removesOnlyThatAlarm_andCancelAll_clearsTheWindow() {
        Prayer.entries.forEach { scheduler.schedule(reminder(it)) }
        scheduler.schedule(reminder(Prayer.FAJR, date = day.plusDays(1)))

        scheduler.cancel(NamazAlarm.Kind.REMINDER, day, Prayer.ASR)
        assertEquals(5, alarms.scheduledAlarms.size)

        scheduler.cancelAll(day)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test
    fun anAlarmsIntent_saysWhichPrayerAndDay() {
        val intent = scheduler.intentFor(NamazAlarm.Kind.REMINDER, day, Prayer.MAGHRIB)

        assertEquals(NamazAlarm.Kind.REMINDER, NamazAlarm.Kind.fromAction(intent.action))
        assertEquals(day, NamazAlarmScheduler.dateFrom(intent))
        assertEquals(Prayer.MAGHRIB, NamazAlarmScheduler.prayerFrom(intent))
    }

    @Test
    fun requestCodes_areUniqueAcrossKindsPrayersAndTheWindowsDays() {
        val codes = NamazAlarm.Kind.entries.flatMap { kind ->
            (-2L..3L).flatMap { offset -> Prayer.entries.map { NamazAlarm.requestCode(kind, day.plusDays(offset), it) } }
        }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun fajrsAlarmClock_isSetAsAnAlarmClock_andTheOthersAreNot() {
        // Alarm clocks need exact alarms too; without them Fajr falls back to the 10-minute window.
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.REMINDER, day, Prayer.FAJR, 5_000L, alarmClock = true))
        scheduler.schedule(reminder(Prayer.DHUHR, at = 6_000L))

        val fajr = alarms.scheduledAlarms.first { it.triggerAtTime == 5_000L }
        val dhuhr = alarms.scheduledAlarms.first { it.triggerAtTime == 6_000L }
        assertTrue(fajr.alarmClockInfo != null)
        assertEquals(null, dhuhr.alarmClockInfo)
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }
}
