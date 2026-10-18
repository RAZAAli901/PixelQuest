package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

/** What happened when the player took a prayed mark back. */
sealed interface UndoPrayerResult {
    /**
     * Back to [status]: pending, or missed if its window has passed meanwhile.
     *
     * @param xpReturned the XP taken back: the prayer's own and any bonus the day loses.
     */
    data class Undone(val status: PrayerStatus, val xpReturned: Int = 0) : UndoPrayerResult

    /** It wasn't marked prayed: nothing to undo. */
    data object NotPrayed : UndoPrayerResult

    /** Only the current prayer day's marks can be taken back. */
    data object PastDay : UndoPrayerResult
}

/**
 * Takes a prayed mark back, on the same prayer day only (docs/namaz-design.md). The prayer goes
 * back to pending, or to missed if its window has passed.
 *
 * Its XP goes back exactly, through the same path it came by (UserProfileRepository.addXp):
 * the prayer's own, and any bonus it unlocked. A day that had all five loses its bonuses whichever
 * prayer is taken back, since it no longer has five, so marking it again can't earn them twice.
 */
class UndoPrayerUseCase @Inject constructor(
    private val provider: PrayerTimeProvider,
    private val repository: NamazRepository,
    private val nextPrayer: NextPrayerUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val syncScheduler: com.pixelquest.app.worker.SyncScheduler? = null
) {
    suspend operator fun invoke(day: LocalDate, prayer: Prayer, now: ZonedDateTime): UndoPrayerResult {
        if (day != nextPrayer(now).prayerDay) return UndoPrayerResult.PastDay
        val logs = repository.getDay(day)
        val row = logs.firstOrNull { it.prayer == prayer && it.status == PrayerStatus.PRAYED } ?: return UndoPrayerResult.NotPrayed

        val times = provider.timesFor(day)
        val windowEnd = prayer.nextInDay?.let { times.timeOf(it) } ?: provider.timesFor(day.plusDays(1)).fajr
        val status = if (now.isBefore(windowEnd)) PrayerStatus.PENDING else PrayerStatus.MISSED
        // The prayer's own XP, and every bonus the day holds (on this prayer or the one that made five).
        val returned = row.xpAwarded + logs.sumOf { it.bonusXp }
        repository.undoPrayer(day, prayer, status)
        repository.clearBonuses(day)
        if (returned > 0) {
            userProfileRepository.addXp(-returned)
            syncScheduler?.scheduleProfileSync()
        }
        return UndoPrayerResult.Undone(status, returned)
    }
}
