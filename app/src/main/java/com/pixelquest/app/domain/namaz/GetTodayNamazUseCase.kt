package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.domain.repository.NamazRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * One prayer on a prayer day, as the tracker shows it.
 *
 * @param started its time has come (it can be marked).
 * @param windowEnd when the next prayer starts: after that, unmarked, it's missed.
 */
data class PrayerSlot(
    val prayer: Prayer,
    val time: ZonedDateTime,
    val windowEnd: ZonedDateTime,
    val status: PrayerStatus,
    val started: Boolean,
    val prayedAt: Long? = null
)

/** A prayer day's five prayers. */
data class NamazDay(val date: LocalDate, val times: PrayerDayTimes, val slots: List<PrayerSlot>) {
    val prayedCount: Int get() = slots.count { it.status == PrayerStatus.PRAYED }
}

/** A prayer day's times with what's been logged for it, as of a moment (docs/namaz-design.md). */
class GetTodayNamazUseCase @Inject constructor(
    private val provider: PrayerTimeProvider,
    private val repository: NamazRepository
) {
    suspend operator fun invoke(day: LocalDate, now: ZonedDateTime): NamazDay {
        val times = provider.timesFor(day)
        val nextFajr = provider.timesFor(day.plusDays(1)).fajr
        return combine(times, nextFajr, repository.getDay(day), now)
    }

    companion object {
        /**
         * [times] with [logs] at [now]: an unmarked prayer whose window has passed (the next prayer
         * has started; for Isha, [nextFajr]) shows as missed even before the missed worker writes it.
         */
        fun combine(times: PrayerDayTimes, nextFajr: ZonedDateTime, logs: List<NamazLogEntity>, now: ZonedDateTime): NamazDay {
            val byPrayer = logs.associateBy { it.prayer }
            val slots = Prayer.entries.map { prayer ->
                val time = times.timeOf(prayer)
                val windowEnd = prayer.nextInDay?.let { times.timeOf(it) } ?: nextFajr
                val logged = byPrayer[prayer]
                val status = when {
                    logged?.status == PrayerStatus.PRAYED -> PrayerStatus.PRAYED
                    !now.isBefore(windowEnd) -> PrayerStatus.MISSED
                    else -> logged?.status ?: PrayerStatus.PENDING
                }
                PrayerSlot(
                    prayer = prayer,
                    time = time,
                    windowEnd = windowEnd,
                    status = status,
                    started = !now.isBefore(time),
                    prayedAt = logged?.prayedAt
                )
            }
            return NamazDay(times.date, times, slots)
        }
    }
}
