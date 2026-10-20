package com.pixelquest.app.study.ui

import com.pixelquest.app.study.data.remote.StudyItemDraft
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.StudyRules
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Why an item's form can't be saved yet, as the server's rules would refuse it. */
enum class ItemFormProblem { TITLE_SHORT, QUIZ_NEEDS_DATE }

/**
 * The Add item (or Edit item) sheet's fields. Typing stops at each field's limit, so only a title
 * too short, or a quiz without a date, can stand in the way ([problem]).
 *
 * @param editingId the item being changed, or null for a new one.
 * @param dueDay the day it's due, or null for none; [dueTime] that day's time.
 * @param pinned to be pinned; only the leader sees this.
 */
data class ItemForm(
    val editingId: String? = null,
    val kind: StudyItemKind = StudyItemKind.ASSIGNMENT,
    val title: String = "",
    val subject: String = "",
    val notes: String = "",
    val dueDay: LocalDate? = null,
    val dueTime: LocalTime = DEFAULT_TIME,
    val pinned: Boolean = false,
    val saving: Boolean = false,
    /** The player tried to save it with a [problem]: now it shows. */
    val triedToSave: Boolean = false
) {
    val problem: ItemFormProblem?
        get() = when {
            title.trim().length < StudyRules.TITLE_MIN -> ItemFormProblem.TITLE_SHORT
            kind == StudyItemKind.QUIZ && dueDay == null -> ItemFormProblem.QUIZ_NEEDS_DATE
            else -> null
        }

    fun withTitle(text: String) = copy(title = text.take(StudyRules.TITLE_MAX))
    fun withSubject(text: String) = copy(subject = text.take(StudyRules.SUBJECT_MAX))
    fun withNotes(text: String) = copy(notes = text.take(StudyRules.NOTES_MAX))

    /** What's sent: its fields trimmed, the due moment in the player's [zone]. */
    fun draft(zone: ZoneId) = StudyItemDraft(
        kind = kind,
        title = title.trim(),
        subject = subject.trim().ifEmpty { null },
        notes = notes.trim().ifEmpty { null },
        dueAt = dueDay?.atTime(dueTime)?.atZone(zone)?.toInstant()
    )

    companion object {
        /** A due time when none is chosen: the end of the day. */
        val DEFAULT_TIME: LocalTime = LocalTime.of(23, 59)

        /** The times offered, by the hour through the day, then the end of it. */
        val TIMES: List<LocalTime> = (7..23).map { LocalTime.of(it, 0) } + DEFAULT_TIME

        /** The days offered: from [today], four weeks of them, with [keep] (an item's own day) among them. */
        fun days(today: LocalDate, keep: LocalDate? = null): List<LocalDate> =
            ((0L until 28L).map { today.plusDays(it) } + listOfNotNull(keep)).distinct().sorted()

        /** The form for changing [item], as it is. */
        fun of(item: StudyItem, zone: ZoneId): ItemForm {
            val due = item.dueAt?.atZone(zone)
            return ItemForm(
                editingId = item.id,
                kind = item.kind,
                title = item.title,
                subject = item.subject.orEmpty(),
                notes = item.notes.orEmpty(),
                dueDay = due?.toLocalDate(),
                dueTime = due?.toLocalTime()?.withSecond(0)?.withNano(0) ?: DEFAULT_TIME,
                pinned = item.pinned
            )
        }
    }
}
