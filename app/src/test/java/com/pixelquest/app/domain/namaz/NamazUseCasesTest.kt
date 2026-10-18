package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The prayer-day rules (docs/namaz-design.md) in Lahore on 17 October 2026, whose times are about
 * Fajr 04:46, Dhuhr 11:48, Asr 15:50, Maghrib 17:29 and Isha 18:50. Each moment used is well
 * away from a prayer's start, so a minute's difference in the calculation doesn't matter.
 */
class NamazUseCasesTest {

    private val lahoreZone = ZoneId.of("Asia/Karachi")
    private val day = LocalDate.of(2026, 10, 17)
    private val yesterday = day.minusDays(1)
    private val tomorrow = day.plusDays(1)

    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
    private val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), FixedClock()) { lahoreZone }
    private val nextPrayer = NextPrayerUseCase(provider)
    private val getToday = GetTodayNamazUseCase(provider, repository)
    private val profile = com.pixelquest.app.testing.FakeUserProfileRepository()
    private val mark = MarkPrayerUseCase(provider, repository, nextPrayer, profile)
    private val undo = UndoPrayerUseCase(provider, repository, nextPrayer, profile)
    private val autoMarkMissed = AutoMarkMissedUseCase(provider, repository, nextPrayer)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): ZonedDateTime =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(lahoreZone)

    // NextPrayerUseCase

    @Test
    fun inTheAfternoon_dhuhrIsCurrent_andAsrIsNext() = runBlocking {
        val now = at(day, 13)
        val result = nextPrayer(now)

        assertEquals(Prayer.DHUHR, result.current.prayer)
        assertEquals(Prayer.ASR, result.next.prayer)
        assertEquals(day, result.prayerDay)
        assertEquals(Duration.between(now, provider.timesFor(day).asr), result.remaining)
    }

    @Test
    fun afterMidnight_beforeFajr_itIsStillYesterdaysPrayerDay() = runBlocking {
        val result = nextPrayer(at(tomorrow, 3))

        assertEquals(PrayerMoment(Prayer.ISHA, day, provider.timesFor(day).isha), result.current)
        assertEquals(PrayerMoment(Prayer.FAJR, tomorrow, provider.timesFor(tomorrow).fajr), result.next)
        assertEquals(day, result.prayerDay)
    }

    @Test
    fun lateAtNight_theNextPrayerIsTomorrowsFajr() = runBlocking {
        val result = nextPrayer(at(day, 23, 30))

        assertEquals(Prayer.ISHA, result.current.prayer)
        assertEquals(PrayerMoment(Prayer.FAJR, tomorrow, provider.timesFor(tomorrow).fajr), result.next)
        assertEquals(day, result.prayerDay)
    }

    // GetTodayNamazUseCase

    @Test
    fun theDay_showsPrayedMissedAndPending_asOfNow() = runBlocking {
        repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = at(day, 5).toInstant().toEpochMilli())

        val shown = getToday(day, at(day, 16))
        val statuses = shown.slots.associate { it.prayer to it.status }

        assertEquals(Prayer.entries, shown.slots.map { it.prayer })
        assertEquals(PrayerStatus.PRAYED, statuses[Prayer.FAJR])
        // Asr started at 15:50, so Dhuhr's window has passed with nothing marked.
        assertEquals(PrayerStatus.MISSED, statuses[Prayer.DHUHR])
        assertEquals(PrayerStatus.PENDING, statuses[Prayer.ASR])
        assertEquals(PrayerStatus.PENDING, statuses[Prayer.MAGHRIB])
        assertEquals(listOf(true, true, true, false, false), shown.slots.map { it.started })
        assertEquals(1, shown.prayedCount)
        // Isha's window runs to tomorrow's Fajr.
        assertEquals(provider.timesFor(tomorrow).fajr, shown.slots.last().windowEnd)
    }

    // MarkPrayerUseCase

    @Test
    fun aPrayerThatHasStarted_isMarkedPrayed_once() = runBlocking {
        val now = at(day, 16)

        assertEquals(MarkPrayerResult.Marked(NamazXpAward(10)), mark(day, Prayer.ASR, now))
        assertEquals(MarkPrayerResult.AlreadyPrayed, mark(day, Prayer.ASR, now.plusMinutes(5)))

        val row = repository.rows.value[day to Prayer.ASR]!!
        assertEquals(PrayerStatus.PRAYED, row.status)
        assertEquals(now.toInstant().toEpochMilli(), row.prayedAt)
        assertEquals(10, row.xpAwarded)
        // Through the game's XP, once.
        assertEquals(10, profile.profile.value!!.totalXp)
    }

    @Test
    fun aPrayerThatHasNotStarted_cannotBeMarked() = runBlocking {
        val result = mark(day, Prayer.MAGHRIB, at(day, 16))

        assertEquals(MarkPrayerResult.NotStartedYet(provider.timesFor(day).maghrib), result)
        assertTrue(mark(tomorrow, Prayer.FAJR, at(day, 16)) is MarkPrayerResult.NotStartedYet)
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun yesterdaysPrayers_canBeMarkedLate_butNotOlderOnes() = runBlocking {
        val now = at(day, 16)

        // Late: its window ended at today's Fajr, so half.
        assertEquals(MarkPrayerResult.Marked(NamazXpAward(5)), mark(yesterday, Prayer.ISHA, now))
        assertEquals(MarkPrayerResult.TooOld, mark(yesterday.minusDays(1), Prayer.ISHA, now))
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(yesterday, Prayer.ISHA))
        assertNull(repository.statusOf(yesterday.minusDays(1), Prayer.ISHA))
    }

    @Test
    fun beforeFajr_ishaIsMarkedOnThePreviousPrayerDay() = runBlocking {
        // Still in its window, which runs to the next Fajr.
        assertEquals(MarkPrayerResult.Marked(NamazXpAward(10)), mark(day, Prayer.ISHA, at(tomorrow, 2)))
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.ISHA))
    }

    // UndoPrayerUseCase

    @Test
    fun undo_returnsToPending_whileTheWindowIsOpen_andToMissedAfter() = runBlocking {
        val now = at(day, 16)
        mark(day, Prayer.DHUHR, at(day, 13))
        mark(day, Prayer.ASR, now)

        assertEquals(UndoPrayerResult.Undone(PrayerStatus.PENDING, xpReturned = 10), undo(day, Prayer.ASR, now))
        assertEquals(UndoPrayerResult.Undone(PrayerStatus.MISSED, xpReturned = 10), undo(day, Prayer.DHUHR, now))
        assertEquals(0, profile.profile.value!!.totalXp)
        assertEquals(PrayerStatus.PENDING, repository.statusOf(day, Prayer.ASR))
        assertEquals(PrayerStatus.MISSED, repository.statusOf(day, Prayer.DHUHR))
        assertNull(repository.rows.value[day to Prayer.ASR]!!.prayedAt)
    }

    @Test
    fun undo_onlyTakesBackPrayedMarks_onTheCurrentPrayerDay() = runBlocking {
        val now = at(day, 16)
        mark(yesterday, Prayer.ISHA, now)

        assertEquals(UndoPrayerResult.NotPrayed, undo(day, Prayer.FAJR, now))
        assertEquals(UndoPrayerResult.PastDay, undo(yesterday, Prayer.ISHA, now))
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(yesterday, Prayer.ISHA))
    }

    // AutoMarkMissedUseCase

    @Test
    fun autoMark_writesMissed_forSeededPrayersWhoseWindowHasPassed() = runBlocking {
        repository.seedDay(day)
        repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = at(day, 5).toInstant().toEpochMilli())

        assertEquals(1, autoMarkMissed(at(day, 16)))

        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.FAJR))
        assertEquals(PrayerStatus.MISSED, repository.statusOf(day, Prayer.DHUHR))
        assertEquals(PrayerStatus.PENDING, repository.statusOf(day, Prayer.ASR))
        // A second run has nothing left to do.
        assertEquals(0, autoMarkMissed(at(day, 16, 30)))
    }

    @Test
    fun autoMark_settlesYesterday_onceTodaysFajrHasStarted() = runBlocking {
        repository.seedDay(yesterday)
        repository.seedDay(day)

        // Before Fajr, yesterday's Isha is still open.
        assertEquals(4, autoMarkMissed(at(day, 3)))
        assertEquals(PrayerStatus.PENDING, repository.statusOf(yesterday, Prayer.ISHA))

        assertEquals(1, autoMarkMissed(at(day, 6)))
        assertEquals(PrayerStatus.MISSED, repository.statusOf(yesterday, Prayer.ISHA))
        assertEquals(PrayerStatus.PENDING, repository.statusOf(day, Prayer.FAJR))
    }

    @Test
    fun autoMark_leavesDaysWithoutRows_andDoesNothingWhileOff() = runBlocking {
        assertEquals(0, autoMarkMissed(at(day, 23)))
        assertTrue(repository.rows.value.isEmpty())

        repository.seedDay(day)
        repository.saveSettings(repository.getSettings().copy(enabled = false))
        assertEquals(0, autoMarkMissed(at(day, 23)))
        assertFalse(repository.rows.value.values.any { it.status == PrayerStatus.MISSED })
    }

    @Test
    fun theFifthPrayer_earnsTheAllFiveBonus_throughTheGamesXp() = runBlocking {
        // Fajr to Maghrib on time, then Isha.
        Prayer.entries.forEach { prayer ->
            val time = provider.timesFor(day).timeOf(prayer).plusMinutes(5)
            val result = mark(day, prayer, time) as MarkPrayerResult.Marked
            val expected = if (prayer == Prayer.ISHA) NamazXpAward(10, allFiveBonus = 30) else NamazXpAward(10)
            assertEquals(expected, result.xp)
        }
        assertEquals(NamazXpConfig.DAILY_CAP, profile.profile.value!!.totalXp)
    }

    @Test
    fun aSeventhAllFiveDayInARow_earnsTheWeeksBonus() = runBlocking {
        // Six full days before this one.
        (1L..6L).forEach { back -> Prayer.entries.forEach { repository.logPrayer(day.minusDays(back), it, prayedAtMillis = 0) } }
        Prayer.entries.dropLast(1).forEach { repository.logPrayer(day, it, prayedAtMillis = 0, xpAwarded = 10) }

        val isha = mark(day, Prayer.ISHA, provider.timesFor(day).isha.plusMinutes(5)) as MarkPrayerResult.Marked

        assertEquals(NamazXpAward(10, allFiveBonus = 30, weekBonus = 100), isha.xp)
    }

    @Test
    fun theFifthPrayersRow_keepsItsOwnXp_andTheBonusApart() = runBlocking {
        Prayer.entries.forEach { mark(day, it, provider.timesFor(day).timeOf(it).plusMinutes(5)) }

        val isha = repository.rows.value[day to Prayer.ISHA]!!
        assertEquals(10, isha.xpAwarded)
        assertEquals(30, isha.bonusXp)
        assertEquals(0, repository.rows.value[day to Prayer.FAJR]!!.bonusXp)
    }

    private fun markAllFive() = runBlocking {
        Prayer.entries.forEach { mark(day, it, provider.timesFor(day).timeOf(it).plusMinutes(5)) }
    }

    @Test
    fun undoingTheFifth_takesBackItsXp_andTheBonusItUnlocked() = runBlocking {
        markAllFive()
        val late = provider.timesFor(day).isha.plusMinutes(30)

        assertEquals(UndoPrayerResult.Undone(PrayerStatus.PENDING, xpReturned = 40), undo(day, Prayer.ISHA, late))

        assertEquals(40, profile.profile.value!!.totalXp)
        assertEquals(0, repository.rows.value.values.sumOf { it.bonusXp })
    }

    @Test
    fun undoingAnotherPrayerOfAFullDay_takesTheDaysBonusBackToo_andMarkingItAgainEarnsItOnce() = runBlocking {
        markAllFive()
        val late = provider.timesFor(day).isha.plusMinutes(30)

        // Fajr's 10, and the day's 30 held by Isha.
        assertEquals(UndoPrayerResult.Undone(PrayerStatus.MISSED, xpReturned = 40), undo(day, Prayer.FAJR, late))
        assertEquals(40, profile.profile.value!!.totalXp)
        assertEquals(0, repository.rows.value[day to Prayer.ISHA]!!.bonusXp)

        // Marked again, late: half, and the bonus comes back with it, once.
        val again = mark(day, Prayer.FAJR, late) as MarkPrayerResult.Marked
        assertEquals(NamazXpAward(5, allFiveBonus = 30), again.xp)
        assertEquals(75, profile.profile.value!!.totalXp)
        assertEquals(30, repository.rows.value.values.sumOf { it.bonusXp })
    }

    @Test
    fun aPrayerMarkedFromEverywhereAtOnce_earnsItsXpOnce() = runBlocking {
        val now = at(day, 16)

        // Ten marks at the same moment: double taps, the screen and a reminder's button.
        val results = (1..10).map {
            async(Dispatchers.Default) { mark(day, Prayer.ASR, now) }
        }.map { it.await() }

        assertEquals(1, results.count { it is MarkPrayerResult.Marked })
        assertEquals(9, results.count { it == MarkPrayerResult.AlreadyPrayed })
        assertEquals(10, profile.profile.value!!.totalXp)
    }

    @Test
    fun markingAndUndoingAtOnce_neverLeavesXpForAnUnmarkedPrayer() = runBlocking {
        val now = at(day, 16)
        repeat(20) {
            val a = async(Dispatchers.Default) { mark(day, Prayer.ASR, now) }
            val b = async(Dispatchers.Default) { undo(day, Prayer.ASR, now) }
            a.await(); b.await()
            val prayed = repository.statusOf(day, Prayer.ASR) == PrayerStatus.PRAYED
            assertEquals(if (prayed) 10 else 0, profile.profile.value!!.totalXp)
        }
    }
}
