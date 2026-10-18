package com.pixelquest.app.domain.namaz

import java.time.LocalDate

/**
 * One Namaz alarm: what it's for, which prayer on which prayer day, and when it fires
 * (docs/namaz-design.md, Reminders).
 */
data class NamazAlarm(val kind: Kind, val date: LocalDate, val prayer: Prayer, val atMillis: Long) {

    /**
     * What an alarm does when it fires. Each kind has its own broadcast action and block of request
     * codes, so a prayer's reminder, nudge and snooze can all be armed at once without replacing
     * each other (Android matches a PendingIntent by its request code and its intent's action).
     */
    enum class Kind(val action: String, val codeBase: Int) {
        /** At the prayer's start (less the offset). */
        REMINDER("com.pixelquest.app.action.NAMAZ_REMINDER", 700_000),
        /** "Still not prayed", before the next prayer starts. */
        NUDGE("com.pixelquest.app.action.NAMAZ_NUDGE", 710_000),
        /** The reminder again, 10 minutes after Snooze. */
        SNOOZE("com.pixelquest.app.action.NAMAZ_SNOOZE", 720_000),
        /** The debug build's test reminder. */
        TEST("com.pixelquest.app.action.NAMAZ_TEST", 730_000);

        companion object {
            fun fromAction(action: String?): Kind? = entries.firstOrNull { it.action == action }
        }
    }

    /** This alarm's identity: arming it again replaces it rather than adding a second one. */
    val requestCode: Int get() = requestCode(kind, date, prayer)

    companion object {
        /**
         * Stable for a kind, a prayer day and a prayer. Dates repeat every 100 days, far beyond the
         * 48 hours that are ever armed, and stay clear of the task reminders' codes (task ids).
         */
        fun requestCode(kind: Kind, date: LocalDate, prayer: Prayer): Int =
            kind.codeBase + Math.floorMod(date.toEpochDay(), 100L).toInt() * 10 + prayer.ordinal
    }
}
