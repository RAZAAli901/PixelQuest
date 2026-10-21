package com.pixelquest.app.study.domain

import java.time.Instant

/**
 * One alert of one occurrence of a reminder, as this phone arms it (docs/study-reminders.md, When
 * it alerts). [index] is which of the reminder's alerts it is; the same index of the next
 * occurrence replaces it once this one has passed.
 */
data class ReminderAlert(
    val reminderId: String,
    val partyId: String,
    val occurrence: ReminderOccurrence,
    val at: Instant,
    val index: Int
) {
    /** Which alarm it is on the phone: one per reminder and index. */
    val slot: String get() = "$reminderId/$index"

    /** This alert of this occurrence, once fired: it's never fired again. */
    val firedKey: String get() = "$reminderId/${occurrence.start.toInstant().toEpochMilli()}/$index"
}

object ReminderAlerts {
    /** How long after it, an alert missed while the phone was off still comes (at a restart). */
    val CATCH_UP: java.time.Duration = java.time.Duration.ofHours(1)

    /** How many occurrences on a reminder is looked at for one with an alert still to come. */
    private const val LOOK_AHEAD = 2

    /**
     * When [occurrence]'s alerts are, in order: the creator's own minutes before its start, or the
     * defaults for its scope.
     */
    fun timesFor(reminder: StudyReminder, occurrence: ReminderOccurrence): List<Instant> {
        val start = occurrence.start
        val custom = reminder.alertOffsets
        val times = if (!custom.isNullOrEmpty()) {
            custom.map { start.minusMinutes(it.toLong()).toInstant() }
        } else {
            defaultTimes(occurrence)
        }
        return times.distinct().sorted()
    }

    /** The defaults, until each scope has its own: the start itself. */
    private fun defaultTimes(occurrence: ReminderOccurrence): List<Instant> = listOf(occurrence.start.toInstant())

    /** Alert [index] of [reminder]'s occurrence starting at [occurrenceStart]; null if there's no such alert now. */
    fun alert(reminder: StudyReminder, occurrenceStart: Instant, index: Int): ReminderAlert? {
        val occurrence = reminder.occurrencesIn(occurrenceStart, occurrenceStart.plusMillis(1), limit = 1)
            .firstOrNull { it.start.toInstant() == occurrenceStart } ?: return null
        val at = timesFor(reminder, occurrence).getOrNull(index) ?: return null
        return ReminderAlert(reminder.id, reminder.partyId, occurrence, at, index)
    }

    /**
     * The alerts of [reminder] to arm at [now]: those still to come of its next occurrence, or, once
     * they've all passed, of the one after. With [catchUpSince], one that came since then counts as
     * still to come (missed while the phone was off).
     */
    fun upcoming(reminder: StudyReminder, now: Instant, catchUpSince: Instant? = null): List<ReminderAlert> {
        val since = catchUpSince ?: now
        var from = now
        repeat(LOOK_AHEAD) {
            val occurrence = reminder.nextOccurrence(from) ?: return emptyList()
            val alerts = timesFor(reminder, occurrence).mapIndexed { index, at -> ReminderAlert(reminder.id, reminder.partyId, occurrence, at, index) }
            val toCome = alerts.filter { it.at.isAfter(since) }
            if (toCome.isNotEmpty()) return toCome
            // This one's alerts are behind it: the next occurrence's, if it repeats.
            if (reminder.repeat == ReminderRepeat.NONE) return emptyList()
            // Over at its end (a time's is its start), so the next one after it.
            from = occurrence.end.toInstant()
        }
        return emptyList()
    }
}
