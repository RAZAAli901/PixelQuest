package com.pixelquest.app.study.ui

import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderProblem
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderRules
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.study.domain.problem
import com.pixelquest.app.study.domain.weekStartOf
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The Create reminder (or Edit reminder) sheet's fields (docs/study-reminders.md). Its moment is
 * worked out in the player's own time zone: a week by its Monday, a day by its start, a time as is.
 *
 * @param editingId the reminder being changed, or null for a new one; its audience can't change.
 * @param alertOffsets the creator's own alert times, minutes before; null for the defaults.
 * @param alertsLocked set when someone else's reminder is open (the leader's edit): its alerts are its creator's.
 */
data class ReminderForm(
    val editingId: String? = null,
    val kind: ReminderKind = ReminderKind.QUIZ,
    val title: String = "",
    val scope: ReminderScope = ReminderScope.WEEK,
    val weekStart: LocalDate,
    val day: LocalDate,
    val time: LocalTime = DEFAULT_TIME,
    val repeat: ReminderRepeat = ReminderRepeat.NONE,
    val audience: ReminderAudience = ReminderAudience.PARTY,
    val alertOffsets: List<Int>? = null,
    val alertsLocked: Boolean = false,
    val saving: Boolean = false,
    /** The player tried to save it with a problem: now it shows. */
    val triedToSave: Boolean = false
) {
    fun withTitle(text: String) = copy(title = com.pixelquest.app.domain.validation.InputRules.typed(text, com.pixelquest.app.domain.validation.InputRules.Field.REMINDER_TITLE))

    /** A week reminder can't repeat daily: choosing a week takes that back to once. */
    fun withScope(next: ReminderScope) =
        copy(scope = next, repeat = if (next == ReminderScope.WEEK && repeat == ReminderRepeat.DAILY) ReminderRepeat.NONE else repeat)

    /** Back to the default alert times. */
    fun withDefaultAlerts() = copy(alertOffsets = null)

    /**
     * Adds or takes away an alert [minutes] before. The last one taken away is the defaults again;
     * past [ReminderRules.ALERTS_MAX], nothing more is added.
     */
    fun toggleAlert(minutes: Int): ReminderForm {
        val now = alertOffsets.orEmpty()
        val next = when {
            minutes in now -> now - minutes
            now.size >= ReminderRules.ALERTS_MAX -> now
            else -> (now + minutes).sortedDescending()
        }
        return copy(alertOffsets = next.ifEmpty { null })
    }

    /** What's sent: its moment in [zone], whose name goes with it. */
    fun draft(zone: ZoneId) = StudyReminderDraft(
        title = title.trim(),
        kind = kind,
        scope = scope,
        dueAt = when (scope) {
            ReminderScope.WEEK -> null
            ReminderScope.DAY -> day.atStartOfDay(zone).toInstant()
            ReminderScope.TIME -> day.atTime(time).atZone(zone).toInstant()
        },
        weekStart = if (scope == ReminderScope.WEEK) weekStart else null,
        tz = zone.id,
        repeat = repeat,
        audience = audience,
        alertOffsets = alertOffsets
    )

    fun problem(zone: ZoneId): ReminderProblem? = draft(zone).problem()

    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)

        /** The alert times offered, in minutes before: from the moment itself to a week. */
        val ALERT_CHOICES: List<Int> = listOf(0, 15, 30, 60, 120, 24 * 60, 2 * 24 * 60, 7 * 24 * 60)

        /** The times offered, by the hour from 6 am to 11 pm. */
        val TIMES: List<LocalTime> = (6..23).map { LocalTime.of(it, 0) }

        /** A new reminder: this week, or today. */
        fun new(today: LocalDate) = ReminderForm(weekStart = weekStartOf(today), day = today)

        /** The weeks offered: this one and the next three, with [keep] (a reminder's own) among them. */
        fun weeks(today: LocalDate, keep: LocalDate? = null): List<LocalDate> =
            ((0L until 4L).map { weekStartOf(today).plusWeeks(it) } + listOfNotNull(keep)).distinct().sorted()

        /** The days offered: four weeks from [today], with [keep] among them. */
        fun days(today: LocalDate, keep: LocalDate? = null): List<LocalDate> =
            ((0L until 28L).map { today.plusDays(it) } + listOfNotNull(keep)).distinct().sorted()

        /** The form for changing [reminder], as it is, read in [zone]; [myId] may set its alerts only if it made it. */
        fun of(reminder: StudyReminder, today: LocalDate, zone: ZoneId, myId: String? = reminder.createdBy): ReminderForm {
            val at = reminder.dueAt?.atZone(zone)
            return ReminderForm(
                editingId = reminder.id,
                kind = reminder.kind,
                title = reminder.title,
                scope = reminder.scope,
                weekStart = reminder.weekStart ?: weekStartOf(today),
                day = at?.toLocalDate() ?: today,
                time = at?.toLocalTime()?.withSecond(0)?.withNano(0) ?: DEFAULT_TIME,
                repeat = reminder.repeat,
                audience = reminder.audience,
                alertOffsets = reminder.alertOffsets,
                alertsLocked = myId == null || reminder.createdBy != myId
            )
        }
    }
}
