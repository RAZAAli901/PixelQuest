package com.pixelquest.app.darood.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** One member's place in a closed Darood week (docs/darood-weekly.md, Positions): ties share it. */
data class WeekPlace(val userId: String, val position: Int, val weekTotal: Long, val activeDays: Int)

/**
 * A Darood Party's closed week, from its Monday [weekStart]: every ranked member's place, stored
 * once on the server and never changed. [places] are in display order: position, then most active
 * days (the username, the last tie-break, is the screen's).
 */
data class WeekResult(val partyId: String, val weekStart: LocalDate, val places: List<WeekPlace>, val closedAt: Instant) {
    val members: Int get() = places.size

    fun placeOf(userId: String?): WeekPlace? = places.firstOrNull { it.userId == userId }

    /** The lowest place anyone holds that week: everyone on it is "last" (ties included). */
    val lastPosition: Int get() = places.maxOfOrNull { it.position } ?: 0
}

/** The Darood week: Monday to Sunday, as the server counts it. */
object DaroodWeeks {
    fun mondayOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** The latest week that's over on [today]: last week's Monday. */
    fun lastClosed(today: LocalDate): LocalDate = mondayOf(today).minusWeeks(1)

    /** How many weeks the ranking screen shows (J079). */
    const val HISTORY_WEEKS = 8
}

/**
 * The kind line under a player's place (docs/darood-weekly.md, Tone): never a shaming one. It
 * depends only on the week's numbers, never on who leads the party, so the leader reads what
 * anyone in that place would.
 */
enum class WeekTone {
    /** First (alone or tied) with Darood to show for it. */
    TOP,
    /** Last place, or no Darood that week: an encouraging line. */
    ENCOURAGE,
    /** Anywhere between: no extra line. */
    NONE;

    companion object {
        fun of(week: WeekResult, userId: String): WeekTone? {
            val mine = week.placeOf(userId) ?: return null
            return when {
                mine.weekTotal == 0L -> ENCOURAGE
                mine.position == 1 -> TOP
                mine.position == week.lastPosition -> ENCOURAGE
                else -> NONE
            }
        }
    }
}
