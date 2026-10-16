package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

/** What happened when the player took a prayed mark back. */
sealed interface UndoPrayerResult {
    /** Back to [status]: pending, or missed if its window has passed meanwhile. */
    data class Undone(val status: PrayerStatus) : UndoPrayerResult

    /** It wasn't marked prayed: nothing to undo. */
    data object NotPrayed : UndoPrayerResult

    /** Only the current prayer day's marks can be taken back. */
    data object PastDay : UndoPrayerResult
}

/**
 * Takes a prayed mark back, on the same prayer day only (docs/namaz-design.md). The prayer goes
 * back to pending, or to missed if its window has passed. Phase 6 also reverses the XP it gave.
 */
class UndoPrayerUseCase @Inject constructor(
    private val provider: PrayerTimeProvider,
    private val repository: NamazRepository,
    private val nextPrayer: NextPrayerUseCase
) {
    suspend operator fun invoke(day: LocalDate, prayer: Prayer, now: ZonedDateTime): UndoPrayerResult {
        if (day != nextPrayer(now).prayerDay) return UndoPrayerResult.PastDay
        if (repository.getDay(day).none { it.prayer == prayer && it.status == PrayerStatus.PRAYED }) {
            return UndoPrayerResult.NotPrayed
        }
        val times = provider.timesFor(day)
        val windowEnd = prayer.nextInDay?.let { times.timeOf(it) } ?: provider.timesFor(day.plusDays(1)).fajr
        val status = if (now.isBefore(windowEnd)) PrayerStatus.PENDING else PrayerStatus.MISSED
        repository.undoPrayer(day, prayer, status)
        return UndoPrayerResult.Undone(status)
    }
}
