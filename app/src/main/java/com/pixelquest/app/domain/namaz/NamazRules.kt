package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazLogEntity
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * What can be marked and taken back, and when (docs/namaz-design.md, XP). The screen, the reminder's
 * buttons and every later way in go through Mark/UndoPrayerUseCase, which apply these.
 */
object NamazRules {

    /** Phone clocks drift and get corrected: this much behind a mark is still fine. */
    const val CLOCK_TOLERANCE_MINUTES = 10L

    /** A prayer is marked from its start on; never before (and never for a day ahead). */
    fun hasStarted(prayerStart: ZonedDateTime, now: ZonedDateTime): Boolean = !now.isBefore(prayerStart)

    /** Marking reaches back to the day before the current prayer day, and no further. */
    fun canMarkDay(day: LocalDate, currentDay: LocalDate): Boolean = !day.isAfter(currentDay) && !day.isBefore(currentDay.minusDays(1))

    /** A mark is taken back on its own prayer day only. */
    fun canUndoDay(day: LocalDate, currentDay: LocalDate): Boolean = day == currentDay

    /**
     * The phone's clock is behind a prayer already marked: it was set ahead to mark prayers early,
     * then back. Marking waits until the real time catches up, so the same prayers can't be farmed
     * for XP again and again.
     */
    fun clockIsBehind(recent: List<NamazLogEntity>, now: ZonedDateTime): Boolean {
        val latest = recent.mapNotNull { it.prayedAt }.maxOrNull() ?: return false
        return latest > now.plusMinutes(CLOCK_TOLERANCE_MINUTES).toInstant().toEpochMilli()
    }
}
