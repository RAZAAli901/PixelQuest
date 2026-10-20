package com.pixelquest.app.study.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** What a study item is, as the server names it (study_items.kind). */
enum class StudyItemKind(val wire: String) {
    ASSIGNMENT("assignment"),
    QUIZ("quiz");

    companion object {
        fun fromWire(wire: String?): StudyItemKind = entries.firstOrNull { it.wire == wire } ?: ASSIGNMENT
    }
}

/** Whether something posted on this phone has reached the server. */
enum class SyncState {
    /** Saved on the phone, waiting to be sent. */
    PENDING,

    /** On the server. */
    SYNCED,

    /** The server refused it (a rule, not the connection): it won't be sent again as it is. */
    FAILED
}

/** The limits the server enforces on a study party's data (docs/study-party.md), for the app to mirror. */
object StudyRules {
    const val TITLE_MIN = 2
    const val TITLE_MAX = 80
    const val SUBJECT_MAX = 40
    const val NOTES_MAX = 500
    const val MINUTES_MIN = 5
    const val MINUTES_MAX = 240

    /** How long a member may undo their own completion; after that it's final. */
    val UNDO_WINDOW: Duration = Duration.ofHours(24)

    /** How far back a completion's or a session's day may be, so those made offline still count. */
    const val DAYS_BACK_MAX = 7L
}

/**
 * Every study points value, as the server has them (study_points_config): a test holds them
 * equal. Points are the party's own and never touch XP, levels or streaks.
 */
object StudyPointsConfig {
    const val ASSIGNMENT = 10
    const val QUIZ = 15
    const val EARLY_BONUS = 5
    const val MINUTES_PER_POINT = 10
    const val DAILY_STUDY_MAX = 12

    /** What finishing an item of [kind] is worth, before any early bonus. */
    fun pointsFor(kind: StudyItemKind): Int = if (kind == StudyItemKind.QUIZ) QUIZ else ASSIGNMENT

    /** The bonus for finishing at [completedAt] an item due at [dueAt]: a day or more early. None without a due moment. */
    fun earlyBonus(dueAt: Instant?, completedAt: Instant): Int =
        if (dueAt != null && !completedAt.isAfter(dueAt.minus(Duration.ofDays(1)))) EARLY_BONUS else 0

    /** The points for a day's study time ([minutes], every session that day together): 1 per 10 minutes, 12 at most. */
    fun studyPoints(minutes: Int): Int = (minutes / MINUTES_PER_POINT).coerceAtMost(DAILY_STUDY_MAX)
}

/** The Monday that starts [date]'s week: weeks run Monday to Sunday, in the member's own days. */
fun weekStartOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Something due in a study party: an assignment or a quiz.
 *
 * @param dueAt when it's due; a quiz always has one, an assignment may not.
 * @param pinned the leader's highlight, shown at the top of the party.
 * @param createdBy who added it; null if that account was deleted.
 */
data class StudyItem(
    val id: String,
    val partyId: String,
    val kind: StudyItemKind,
    val title: String,
    val subject: String?,
    val notes: String?,
    val dueAt: Instant?,
    val pinned: Boolean,
    val createdBy: String?,
    val createdAt: Instant
)

/**
 * A member's tick on an item. [id] is the server's, null until it's there; [clientId] is the
 * phone's own key for it, so sending it twice never counts it twice. [localDate] and [tz] are the
 * member's own day and time zone when they ticked it, which decide the week it counts in.
 */
data class StudyCompletion(
    val id: Long?,
    val clientId: String?,
    val itemId: String,
    val partyId: String,
    val userId: String?,
    val completedAt: Instant,
    val localDate: LocalDate,
    val tz: String,
    val state: SyncState = SyncState.SYNCED
) {
    /** Its owner may still undo it at [now]: within 24 hours of finishing. */
    fun canUndoAt(now: Instant): Boolean = now.isBefore(completedAt.plus(StudyRules.UNDO_WINDOW))
}

/** Study time a member logged: 5 to 240 minutes on their own day, final once logged. */
data class StudySession(
    val id: Long?,
    val clientId: String?,
    val partyId: String,
    val userId: String?,
    val minutes: Int,
    val localDate: LocalDate,
    val tz: String,
    val createdAt: Instant,
    val state: SyncState = SyncState.SYNCED
)

/**
 * A member's points for one week in a study party, with the breakdown (study_week_scores).
 * [username] is theirs where the reader may see it.
 */
data class WeekScore(
    val partyId: String,
    val userId: String,
    val username: String?,
    val weekStart: LocalDate,
    val points: Int,
    val itemPoints: Int,
    val bonusPoints: Int,
    val studyPoints: Int,
    val itemsCompleted: Int,
    val studyMinutes: Int
)

/** A closed week's winner, as the board records it (ties all win). */
data class WeekWinner(val partyId: String, val weekStart: LocalDate, val userId: String, val username: String?, val points: Int)
