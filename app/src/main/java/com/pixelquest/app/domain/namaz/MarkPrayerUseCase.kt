package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

/** What happened when the player marked a prayer. */
sealed interface MarkPrayerResult {
    data object Marked : MarkPrayerResult

    /** Already prayed: nothing changes (a double tap, or the notification action after the screen). */
    data object AlreadyPrayed : MarkPrayerResult

    /** Its time hasn't come; it can be marked from [startsAt]. */
    data class NotStartedYet(val startsAt: ZonedDateTime) : MarkPrayerResult

    /** Before yesterday's prayer day: too far back to change. */
    data object TooOld : MarkPrayerResult
}

/**
 * Marks a prayer prayed (docs/namaz-design.md). Only once its time has started, never for a day
 * ahead, and only on the current prayer day or the one before. A prayer marked after its window
 * passed is still prayed, just late (Phase 6 gives that half XP). No XP yet (Phase 6).
 */
class MarkPrayerUseCase @Inject constructor(
    private val provider: PrayerTimeProvider,
    private val repository: NamazRepository,
    private val nextPrayer: NextPrayerUseCase
) {
    suspend operator fun invoke(day: LocalDate, prayer: Prayer, now: ZonedDateTime): MarkPrayerResult {
        val currentDay = nextPrayer(now).prayerDay
        val time = provider.timesFor(day).timeOf(prayer)
        if (day.isAfter(currentDay) || now.isBefore(time)) return MarkPrayerResult.NotStartedYet(time)
        if (day.isBefore(currentDay.minusDays(1))) return MarkPrayerResult.TooOld
        if (repository.getDay(day).any { it.prayer == prayer && it.status == PrayerStatus.PRAYED }) {
            return MarkPrayerResult.AlreadyPrayed
        }
        repository.logPrayer(day, prayer, prayedAtMillis = now.toInstant().toEpochMilli(), xpAwarded = 0)
        return MarkPrayerResult.Marked
    }
}
