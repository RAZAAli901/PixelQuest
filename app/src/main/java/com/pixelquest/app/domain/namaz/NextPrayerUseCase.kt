package com.pixelquest.app.domain.namaz

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

/** One prayer on one prayer day, at its time. */
data class PrayerMoment(val prayer: Prayer, val day: LocalDate, val time: ZonedDateTime)

/**
 * Where [now] falls among the prayers.
 *
 * @param current the prayer whose time has most recently started (yesterday's Isha before Fajr).
 * @param next the next one to start (tomorrow's Fajr after Isha).
 * @param prayerDay the day [current] belongs to: before Fajr, that's still yesterday.
 */
data class NextPrayer(
    val current: PrayerMoment,
    val next: PrayerMoment,
    val remaining: Duration,
    val prayerDay: LocalDate
)

/** The current prayer, the next, and the time until it, across the day boundary (docs/namaz-design.md). */
class NextPrayerUseCase @Inject constructor(
    private val provider: PrayerTimeProvider
) {
    suspend operator fun invoke(now: ZonedDateTime): NextPrayer {
        val timeline = timeline(now.toLocalDate())
        val current = timeline.last { !it.time.isAfter(now) }
        val next = timeline.first { it.time.isAfter(now) }
        return NextPrayer(current, next, Duration.between(now, next.time), current.day)
    }

    /** Yesterday's, today's and tomorrow's prayers in order: enough to place any moment of today. */
    private suspend fun timeline(today: LocalDate): List<PrayerMoment> =
        listOf(today.minusDays(1), today, today.plusDays(1)).flatMap { day ->
            val times = provider.timesFor(day)
            Prayer.entries.map { PrayerMoment(it, day, times.timeOf(it)) }
        }.sortedBy { it.time.toInstant() }
}
