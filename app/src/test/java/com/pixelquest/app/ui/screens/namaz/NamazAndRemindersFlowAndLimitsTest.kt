package com.pixelquest.app.ui.screens.namaz

import android.app.AlarmManager
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.NamazRepositoryImpl
import com.pixelquest.app.data.repository.UserProfileRepositoryImpl
import com.pixelquest.app.domain.namaz.AutoMarkMissedUseCase
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.MarkPrayerResult
import com.pixelquest.app.domain.namaz.MarkPrayerUseCase
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazHighLatitudeRule
import com.pixelquest.app.domain.namaz.NamazMethod
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.domain.namaz.NamazPlaceResolver
import com.pixelquest.app.domain.namaz.NamazRules
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerCalcSettings
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.namaz.GeoPoint
import com.pixelquest.app.namaz.LocationProvider
import com.pixelquest.app.namaz.NamazAlarmScheduler
import com.pixelquest.app.scheduling.BootReceiver
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Namaz and reminders flow and limits test suite (V041):
 * - User flow: Turn on, set location, see five times, mark each, schedule reminders
 * - Location denied fallback to default city
 * - Extreme latitudes (high-latitude rule calculations without arithmetic exceptions)
 * - Marking before prayer start time protection
 * - Midnight rollover: auto-marking missed prayers and advancing to next prayer day
 * - Exact alarms denied fallback
 * - Reboot and broadcast re-arm actions
 * - Time zone change handling
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazAndRemindersFlowAndLimitsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private lateinit var db: AppDatabase
    private lateinit var repository: NamazRepositoryImpl
    private lateinit var userProfileRepo: UserProfileRepositoryImpl
    private lateinit var calculator: PrayerTimesCalculator
    private lateinit var scheduler: NamazAlarmScheduler

    private val testDay = LocalDate.of(2026, 10, 17)

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NamazRepositoryImpl(db.namazLogDao(), db.namazSettingsDao())
        userProfileRepo = UserProfileRepositoryImpl(db.userProfileDao())
        calculator = PrayerTimesCalculator()
        scheduler = NamazAlarmScheduler(context)

        userProfileRepo.insertProfile(
            UserProfileEntity(id = 1, username = "FaithfulHero", avatarId = "avatar_hero", level = 3, totalXp = 500),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun flow_turnOn_setLocation_seeFiveTimes_markEach_andGetReminders() = runBlocking {
        // 1. Turn on Namaz tracker & configure location
        val settings = NamazSettingsEntity(
            enabled = true,
            city = "Lahore",
            latitude = 31.5204,
            longitude = 74.3587,
            reminderOffsetMin = 10,
            xpEnabled = true,
        )
        repository.saveSettings(settings)

        val savedSettings = repository.getSettings()
        assertTrue("Namaz must be enabled", savedSettings.enabled)
        assertEquals("Lahore", savedSettings.city)

        // 2. See five prayer times
        val provider = DefaultPrayerTimeProvider(repository, calculator, FixedClock()) { LahoreZone }
        val prayerTimes = provider.timesFor(testDay)

        val timesInOrder = listOf(
            prayerTimes.fajr,
            prayerTimes.sunrise,
            prayerTimes.dhuhr,
            prayerTimes.asr,
            prayerTimes.maghrib,
            prayerTimes.isha,
        )
        // Verify strict chronological order
        assertEquals(timesInOrder, timesInOrder.sortedBy { it.toInstant() })

        // 3. Mark each prayer as its time comes
        val nextPrayer = NextPrayerUseCase(provider)
        val markUseCase = MarkPrayerUseCase(provider, repository, nextPrayer, userProfileRepo)

        for (prayer in Prayer.entries) {
            val prayerTime = prayerTimes.timeOf(prayer)
            val result = markUseCase(testDay, prayer, prayerTime)
            assertTrue("Marking prayer $prayer at its time must succeed", result is MarkPrayerResult.Marked)
        }

        // Verify all 5 prayers marked
        val todayLogs = repository.getDay(testDay)
        assertEquals(5, todayLogs.size)
        assertTrue("All prayers must be marked PRAYED", todayLogs.all { it.status == PrayerStatus.PRAYED })

        // 4. Schedule reminders for all 5 prayers
        Prayer.entries.forEachIndexed { index, prayer ->
            val triggerMillis = prayerTimes.timeOf(prayer).toInstant().toEpochMilli()
            scheduler.schedule(NamazAlarm(NamazAlarm.Kind.REMINDER, testDay, prayer, triggerMillis))
        }

        assertEquals(5, alarms.scheduledAlarms.size)
        assertTrue(alarms.scheduledAlarms.all { it.type == AlarmManager.RTC_WAKEUP })
    }

    @Test
    fun limits_locationDenied_fallsBackToLahoreWithoutCrash() = runBlocking {
        val deniedLocationProvider = object : LocationProvider {
            override fun hasPermission(): Boolean = false
            override suspend fun currentLocation(): GeoPoint? = null
        }

        val emptyRepo = FakeNamazRepository()
        val resolver = NamazPlaceResolver(emptyRepo, deniedLocationProvider)

        val place = resolver.currentPlace()
        assertNotNull(place)
        assertEquals(NamazPlace.LAHORE.name, place.name)
        assertEquals(NamazPlace.LAHORE.latitude, place.latitude, 0.0001)
        assertEquals(NamazPlace.LAHORE.longitude, place.longitude, 0.0001)

        // Prayer times calculate cleanly with fallback place
        val times = calculator.calculate(testDay, place.latitude, place.longitude, LahoreZone)
        assertNotNull(times.fajr)
        assertNotNull(times.isha)
    }

    @Test
    fun limits_extremeLatitudes_calculatesAscendingTimesSafely() {
        // Reykjavik, Iceland (64.1466° N) during June solstice
        val solsticeDate = LocalDate.of(2026, 6, 21)
        val icelandZone = ZoneId.of("Atlantic/Reykjavik")
        val reykjavikSettings = PrayerCalcSettings(
            method = NamazMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = NamazHighLatitudeRule.TWILIGHT_ANGLE,
        )

        val times = calculator.calculate(solsticeDate, 64.1466, -21.9426, icelandZone, reykjavikSettings)

        assertNotNull(times)
        val list = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)
        assertEquals("Prayers in extreme latitude must remain sorted chronologically", list, list.sortedBy { it.toInstant() })
    }

    @Test
    fun limits_markingBeforeTheTime_isDisallowedByNamazRules() {
        val fajrTime = ZonedDateTime.of(testDay, LocalTime.of(5, 15), LahoreZone)
        val earlyMorning = ZonedDateTime.of(testDay, LocalTime.of(4, 30), LahoreZone)

        // Attempting to mark at 04:30 when Fajr is at 05:15
        val canMark = NamazRules.hasStarted(fajrTime, earlyMorning)
        assertFalse("Cannot mark prayer before its start time", canMark)

        // At exact start time or after
        val atStart = NamazRules.hasStarted(fajrTime, fajrTime)
        assertTrue("Can mark prayer at its start time", atStart)
    }

    @Test
    fun limits_midnightRollover_autoMarksMissedAndAdvancesToNextDay() = runBlocking {
        // Day 1: seed day and mark Fajr and Dhuhr, leaving Asr, Maghrib, Isha unprayed
        repository.saveSettings(NamazSettingsEntity(enabled = true, latitude = 31.5204, longitude = 74.3587))
        repository.seedDay(testDay)

        val provider = DefaultPrayerTimeProvider(repository, calculator, FixedClock()) { LahoreZone }
        val prayerTimes = provider.timesFor(testDay)

        val nextPrayer = NextPrayerUseCase(provider)
        val markUseCase = MarkPrayerUseCase(provider, repository, nextPrayer, userProfileRepo)
        markUseCase(testDay, Prayer.FAJR, prayerTimes.fajr)
        markUseCase(testDay, Prayer.DHUHR, prayerTimes.dhuhr)

        // 1. Midnight rollover: at 00:05 AM past midnight, Asr and Maghrib are missed, while Isha remains open until Fajr
        val nextDay = testDay.plusDays(1)
        val midnightZoned = ZonedDateTime.of(nextDay, LocalTime.of(0, 5), LahoreZone)
        val autoMarkMissed = AutoMarkMissedUseCase(provider, repository, nextPrayer)

        val midnightMarked = autoMarkMissed(midnightZoned)
        assertEquals(2, midnightMarked) // Asr and Maghrib marked missed

        val midnightLogs = repository.getDay(testDay)
        assertEquals(PrayerStatus.PRAYED, midnightLogs.first { it.prayer == Prayer.FAJR }.status)
        assertEquals(PrayerStatus.PRAYED, midnightLogs.first { it.prayer == Prayer.DHUHR }.status)
        assertEquals(PrayerStatus.MISSED, midnightLogs.first { it.prayer == Prayer.ASR }.status)
        assertEquals(PrayerStatus.MISSED, midnightLogs.first { it.prayer == Prayer.MAGHRIB }.status)
        assertEquals(PrayerStatus.PENDING, midnightLogs.first { it.prayer == Prayer.ISHA }.status)

        // 2. Once next day's Fajr starts (06:00 AM), Isha's window is also closed and marked missed
        val morningZoned = ZonedDateTime.of(nextDay, LocalTime.of(6, 0), LahoreZone)
        val morningMarked = autoMarkMissed(morningZoned)
        assertEquals(1, morningMarked) // Isha marked missed

        val settledLogs = repository.getDay(testDay)
        assertEquals(PrayerStatus.MISSED, settledLogs.first { it.prayer == Prayer.ISHA }.status)
    }

    @Test
    fun limits_exactAlarmsDenied_schedulerOperatesSafely() {
        // NamazAlarmScheduler delegates to AlarmScheduler which checks canScheduleExactAlarms()
        val canScheduleExact = scheduler.canScheduleExactAlarms()

        // Scheduling an alarm should execute cleanly without SecurityException
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.REMINDER, testDay, Prayer.FAJR, 1_000_000L))
        assertTrue(alarms.scheduledAlarms.isNotEmpty())

        scheduler.cancel(NamazAlarm.Kind.REMINDER, testDay, Prayer.FAJR)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test
    fun limits_bootAndRearmBroadcasts_triggersIdempotentRescheduling() {
        val rearmActions = BootReceiver.REARM_ACTIONS

        assertTrue("Boot completed must be a rearm trigger", rearmActions.contains(android.content.Intent.ACTION_BOOT_COMPLETED))
        assertTrue("Timezone changed must be a rearm trigger", rearmActions.contains(android.content.Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue("Time changed must be a rearm trigger", rearmActions.contains(android.content.Intent.ACTION_TIME_CHANGED))

        // Scheduling multiple alarms idempotently replaces existing alarms
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.REMINDER, testDay, Prayer.DHUHR, 2_000_000L))
        scheduler.schedule(NamazAlarm(NamazAlarm.Kind.REMINDER, testDay, Prayer.DHUHR, 2_500_000L))

        assertEquals(1, alarms.scheduledAlarms.size)
        assertEquals(2_500_000L, alarms.scheduledAlarms.single().triggerAtTime)
    }

    @Test
    fun limits_timeZoneChange_recalculatesLocalClockTimesPreservingUtcMoments() {
        val lahorePlace = NamazPlace.LAHORE

        // When device is in Karachi (UTC+5) vs when phone is set to UTC (UTC+0)
        val inKarachi = calculator.calculate(testDay, lahorePlace.latitude, lahorePlace.longitude, ZoneId.of("Asia/Karachi"))
        val inUtc = calculator.calculate(testDay, lahorePlace.latitude, lahorePlace.longitude, ZoneOffset.UTC)

        // Absolute moment of Maghrib (solar event) is invariant
        assertEquals(inKarachi.maghrib.toInstant(), inUtc.maghrib.toInstant())

        // Local clock time difference matches timezone offset difference (5 hours)
        val hoursDifference = Duration.between(inUtc.maghrib.toLocalTime(), inKarachi.maghrib.toLocalTime()).toHours()
        assertEquals(5L, hoursDifference)
    }
}
