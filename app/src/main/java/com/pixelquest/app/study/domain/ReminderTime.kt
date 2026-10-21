package com.pixelquest.app.study.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

// When a reminder's occurrences are (docs/study-reminders.md, Scopes and Repeating), worked out in
// its own time zone: a week from its Monday to the next, a day from its start to the next, a time
// at its moment. A daily or weekly one repeats from its first, keeping its local time across
// changes of clock.

/** The reminder's time zone, or UTC for a name this phone doesn't know. */
val StudyReminder.zone: ZoneId
    get() = runCatching { ZoneId.of(tz) }.getOrDefault(ZoneOffset.UTC)

/** One occurrence of a reminder: when it starts, and when it's over (the same moment for a time). */
data class ReminderOccurrence(val start: ZonedDateTime, val end: ZonedDateTime)

/** The first occurrence's start; null for a row missing its moment (the server never stores one). */
private fun StudyReminder.firstStart(): ZonedDateTime? = when (scope) {
    ReminderScope.WEEK -> weekStart?.atStartOfDay(zone)
    ReminderScope.DAY, ReminderScope.TIME -> dueAt?.atZone(zone)
}

private fun StudyReminder.occurrence(n: Long, first: ZonedDateTime): ReminderOccurrence {
    val start = when (repeat) {
        ReminderRepeat.NONE -> first
        ReminderRepeat.DAILY -> first.plusDays(n)
        ReminderRepeat.WEEKLY -> first.plusWeeks(n)
    }
    val end = when (scope) {
        ReminderScope.WEEK -> start.plusWeeks(1)
        ReminderScope.DAY -> start.plusDays(1)
        ReminderScope.TIME -> start
    }
    return ReminderOccurrence(start, end)
}

/** Days between occurrences: none for a reminder that doesn't repeat. */
private val StudyReminder.periodDays: Long?
    get() = when (repeat) {
        ReminderRepeat.NONE -> null
        ReminderRepeat.DAILY -> 1
        ReminderRepeat.WEEKLY -> 7
    }

/**
 * The occurrences that touch [from] to [to]: a span overlapping it, or a time within it. In order,
 * at most [limit] of them.
 */
fun StudyReminder.occurrencesIn(from: Instant, to: Instant, limit: Int = 31): List<ReminderOccurrence> {
    val first = firstStart() ?: return emptyList()
    val period = periodDays
    if (period == null) {
        val only = occurrence(0, first)
        return if (only.touches(from, to)) listOf(only) else emptyList()
    }
    // Jump close to [from] (a day early, for changes of clock), then walk.
    val behind = ChronoUnit.DAYS.between(first.toInstant(), from) - (if (scope == ReminderScope.WEEK) 7 else 1)
    var n = maxOf(0L, behind / period)
    val found = mutableListOf<ReminderOccurrence>()
    while (found.size < limit) {
        val each = occurrence(n, first)
        if (!each.start.toInstant().isBefore(to)) break
        if (each.touches(from, to)) found += each
        n++
    }
    return found
}

/** The first occurrence that isn't over at [now]: the one its alerts are for. Null once it never comes again. */
fun StudyReminder.nextOccurrence(now: Instant): ReminderOccurrence? {
    val first = firstStart() ?: return null
    val period = periodDays ?: return occurrence(0, first).takeIf { !it.isOver(now) }
    val behind = ChronoUnit.DAYS.between(first.toInstant(), now) - (if (scope == ReminderScope.WEEK) 7 else 1)
    var n = maxOf(0L, behind / period)
    while (true) {
        val each = occurrence(n, first)
        if (!each.isOver(now)) return each
        n++
    }
}

/** Over once its end has passed; a time is over the moment it comes. */
fun ReminderOccurrence.isOver(now: Instant): Boolean = !end.toInstant().isAfter(now)

private fun ReminderOccurrence.touches(from: Instant, to: Instant): Boolean =
    if (start == end) !start.toInstant().isBefore(from) && start.toInstant().isBefore(to)
    else start.toInstant().isBefore(to) && end.toInstant().isAfter(from)
