package com.pixelquest.app.study.domain

import java.time.Instant
import java.time.LocalDate

// A study party member's reminders (docs/study-reminders.md). A reminder only reminds: it never
// touches points, XP, levels or streaks.

/** What a reminder is about, as the server names it (study_reminders.kind). */
enum class ReminderKind(val wire: String) {
    QUIZ("quiz"),
    ASSIGNMENT("assignment"),
    EXAM("exam"),
    CUSTOM("custom");

    companion object {
        fun fromWire(wire: String?): ReminderKind = entries.firstOrNull { it.wire == wire } ?: CUSTOM
    }
}

/** When a reminder is: a whole week, a day, or a day and time (study_reminders.scope). */
enum class ReminderScope(val wire: String) {
    WEEK("week"),
    DAY("day"),
    TIME("time");

    companion object {
        fun fromWire(wire: String?): ReminderScope = entries.firstOrNull { it.wire == wire } ?: TIME
    }
}

/** How a reminder repeats (study_reminders.repeat). Only a day or time reminder repeats daily. */
enum class ReminderRepeat(val wire: String) {
    NONE("none"),
    DAILY("daily"),
    WEEKLY("weekly");

    companion object {
        fun fromWire(wire: String?): ReminderRepeat = entries.firstOrNull { it.wire == wire } ?: NONE
    }
}

/** Who a reminder is for (study_reminders.audience): every active member, or its creator alone. */
enum class ReminderAudience(val wire: String) {
    PARTY("party"),
    ME("me");

    companion object {
        fun fromWire(wire: String?): ReminderAudience = entries.firstOrNull { it.wire == wire } ?: PARTY
    }
}

/**
 * A reminder in a study party.
 *
 * @param weekStart the Monday of its week, for a [ReminderScope.WEEK] reminder; null otherwise.
 * @param dueAt its moment, for a day (the start of that day in [tz]) or time reminder; null for a week.
 * @param tz the creator's time zone: which day and week the reminder means.
 * @param alertOffsets the creator's own alert times, in minutes before the reminder's moment;
 *   null for the defaults (docs/study-reminders.md, Alerts).
 */
data class StudyReminder(
    val id: String,
    val partyId: String,
    val createdBy: String?,
    val title: String,
    val kind: ReminderKind,
    val scope: ReminderScope,
    val dueAt: Instant?,
    val weekStart: LocalDate?,
    val tz: String,
    val repeat: ReminderRepeat,
    val audience: ReminderAudience,
    val alertOffsets: List<Int>?,
    val createdAt: Instant,
    /** Null once the server has it; PENDING while a change made here waits to be sent, FAILED if refused. */
    val sync: SyncState? = null
)

/** The limits the server holds a reminder to (20261019000046, 20261019000048), for the app to mirror. */
object ReminderRules {
    const val TITLE_MIN = 2
    const val TITLE_MAX = 80

    /** At most this many custom alert times. */
    const val ALERTS_MAX = 4

    /** A custom alert is at most this many minutes (14 days) before the reminder. */
    const val OFFSET_MAX_MINUTES = 14 * 24 * 60

    /** A member keeps at most this many reminders in a party. */
    const val PER_MEMBER_MAX = 100
}

/** A reminder as its creator fills it in, before the server has it. */
data class StudyReminderDraft(
    val title: String,
    val kind: ReminderKind,
    val scope: ReminderScope,
    val dueAt: Instant?,
    val weekStart: LocalDate?,
    val tz: String,
    val repeat: ReminderRepeat = ReminderRepeat.NONE,
    val audience: ReminderAudience = ReminderAudience.PARTY,
    val alertOffsets: List<Int>? = null
)

/** What's wrong with a draft, by the server's own rules, so the app can say so before sending. */
enum class ReminderProblem { TITLE, WHEN, REPEAT, ALERTS, TIME_ZONE }

/** The first of the server's rules [this] breaks, or null when it would be stored. */
fun StudyReminderDraft.problem(): ReminderProblem? {
    if (title.trim().length !in ReminderRules.TITLE_MIN..ReminderRules.TITLE_MAX) return ReminderProblem.TITLE
    val whenOk = when (scope) {
        ReminderScope.WEEK -> dueAt == null && weekStart?.dayOfWeek == java.time.DayOfWeek.MONDAY
        ReminderScope.DAY, ReminderScope.TIME -> dueAt != null && weekStart == null
    }
    if (!whenOk) return ReminderProblem.WHEN
    if (repeat == ReminderRepeat.DAILY && scope == ReminderScope.WEEK) return ReminderProblem.REPEAT
    val offsets = alertOffsets
    if (offsets != null && (offsets.size !in 1..ReminderRules.ALERTS_MAX || offsets.any { it !in 0..ReminderRules.OFFSET_MAX_MINUTES })) {
        return ReminderProblem.ALERTS
    }
    if (runCatching { java.time.ZoneId.of(tz) }.isFailure || tz.length > 64) return ReminderProblem.TIME_ZONE
    return null
}
