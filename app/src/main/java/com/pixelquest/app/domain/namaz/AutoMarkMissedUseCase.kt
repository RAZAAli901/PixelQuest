package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Writes MISSED for prayers left pending once their window has passed (the next prayer started),
 * on the current prayer day and the one before (docs/namaz-design.md). Only rows a day was seeded
 * with change: turning the tracker on in the evening doesn't mark that day's earlier prayers
 * missed. Does nothing while the tracker is off.
 *
 * @return how many prayers it marked missed.
 */
class AutoMarkMissedUseCase @Inject constructor(
    private val provider: PrayerTimeProvider,
    private val repository: NamazRepository,
    private val nextPrayer: NextPrayerUseCase
) {
    suspend operator fun invoke(now: ZonedDateTime): Int {
        if (!repository.getSettings().enabled) return 0
        val today = nextPrayer(now).prayerDay
        var marked = 0
        for (day in listOf(today.minusDays(1), today)) {
            val logs = repository.getDay(day)
            if (logs.isEmpty()) continue
            val shown = GetTodayNamazUseCase.combine(provider.timesFor(day), provider.timesFor(day.plusDays(1)).fajr, logs, now)
            for (slot in shown.slots) {
                val stored = logs.firstOrNull { it.prayer == slot.prayer } ?: continue
                if (stored.status == PrayerStatus.PENDING && slot.status == PrayerStatus.MISSED) {
                    repository.setStatus(day, slot.prayer, PrayerStatus.MISSED)
                    marked++
                }
            }
        }
        return marked
    }
}
