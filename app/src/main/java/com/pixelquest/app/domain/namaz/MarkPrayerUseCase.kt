package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

/** What happened when the player marked a prayer. */
sealed interface MarkPrayerResult {
    /** Marked, with the XP it earned (none past the daily cap). */
    data class Marked(val xp: NamazXpAward) : MarkPrayerResult

    /** Already prayed: nothing changes (a double tap, or the notification action after the screen). */
    data object AlreadyPrayed : MarkPrayerResult

    /** Its time hasn't come; it can be marked from [startsAt]. */
    data class NotStartedYet(val startsAt: ZonedDateTime) : MarkPrayerResult

    /** Before yesterday's prayer day: too far back to change. */
    data object TooOld : MarkPrayerResult

    /** The phone's clock is behind a prayer already marked (NamazRules.clockIsBehind). */
    data object ClockBehind : MarkPrayerResult
}

/**
 * Marks a prayer prayed (docs/namaz-design.md), by NamazRules: only once its time has started, never
 * for a day ahead, only on the current prayer day or the one before, and not while the phone's
 * clock is behind a prayer already marked. A prayer marked after its window passed is still
 * prayed, just late, for half the XP.
 *
 * The XP goes through the game's one XP path, UserProfileRepository.addXp (the same as a completed
 * task), and the profile is synced to the leaderboard after. Levels come from perfect days, not XP,
 * so marking a prayer never changes the level and writes no level history.
 */
class MarkPrayerUseCase @Inject constructor(
    private val provider: PrayerTimeProvider,
    private val repository: NamazRepository,
    private val nextPrayer: NextPrayerUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val syncScheduler: com.pixelquest.app.worker.SyncScheduler? = null
) {
    suspend operator fun invoke(day: LocalDate, prayer: Prayer, now: ZonedDateTime): MarkPrayerResult =
        NamazXpLock.mutex.withLock { mark(day, prayer, now) }

    private suspend fun mark(day: LocalDate, prayer: Prayer, now: ZonedDateTime): MarkPrayerResult {
        val currentDay = nextPrayer(now).prayerDay
        val times = provider.timesFor(day)
        val time = times.timeOf(prayer)
        if (day.isAfter(currentDay) || !NamazRules.hasStarted(time, now)) return MarkPrayerResult.NotStartedYet(time)
        if (!NamazRules.canMarkDay(day, currentDay)) return MarkPrayerResult.TooOld
        // Marks from a clock set ahead sit on the days just after (or on) the one it now says.
        if (NamazRules.clockIsBehind(repository.observeRange(currentDay.minusDays(1), currentDay.plusDays(2)).first(), now)) {
            return MarkPrayerResult.ClockBehind
        }
        val logs = repository.getDay(day)
        if (logs.any { it.prayer == prayer && it.status == PrayerStatus.PRAYED }) return MarkPrayerResult.AlreadyPrayed

        val xp = xpFor(day, prayer, now, times, logs)
        repository.logPrayer(day, prayer, prayedAtMillis = now.toInstant().toEpochMilli(), xpAwarded = xp.prayer, bonusXp = xp.bonus)
        if (xp.total > 0) {
            userProfileRepository.addXp(xp.total)
            syncScheduler?.scheduleProfileSync()
        }
        return MarkPrayerResult.Marked(xp)
    }

    /** What marking [prayer] on [day] earns at [now], from what the day already holds. */
    private suspend fun xpFor(
        day: LocalDate,
        prayer: Prayer,
        now: ZonedDateTime,
        times: PrayerDayTimes,
        logs: List<com.pixelquest.app.data.local.entity.NamazLogEntity>
    ): NamazXpAward {
        val windowEnd = prayer.nextInDay?.let { times.timeOf(it) } ?: provider.timesFor(day.plusDays(1)).fajr
        val prayed = logs.filter { it.status == PrayerStatus.PRAYED }
        // The run of all-five days this day would end with, were it to make five now.
        val perDay = repository.observePrayedPerDay(day.minusYears(STREAK_YEARS), day).first()
            .associate { it.date to it.prayed } + (day to NamazStreak.PRAYERS_PER_DAY)
        return NamazXpCalculator.award(
            onTime = now.isBefore(windowEnd),
            prayedBefore = prayed.size,
            earnedToday = prayed.sumOf { it.xpAwarded },
            runIfComplete = NamazStreak.current(perDay, day)
        )
    }

    private companion object {
        /** How far back a run is counted, as on the screen. */
        const val STREAK_YEARS = 10L
    }
}
