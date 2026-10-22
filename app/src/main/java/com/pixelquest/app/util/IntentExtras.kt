package com.pixelquest.app.util

import android.content.Intent
import java.time.LocalDate

/**
 * Reads the extras our alarms, notifications and prompts carry, and refuses anything that isn't
 * the shape we put there (S045). Every component that reads them is unexported, but a stale
 * PendingIntent from an older version, or a bug, must never reach the database or the UI.
 */
object IntentExtras {
    /** Longer than any quest name the app saves; a label past it is cut, never shown whole. */
    const val MAX_LABEL = 200

    /** Days since 1970 up to the year 2243: any real alarm date fits, LocalDate never throws. */
    private const val MAX_EPOCH_DAY = 100_000L

    private val UUID = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")

    /** A Room row id: always positive. */
    fun rowId(intent: Intent?, key: String): Long? =
        intent?.getLongExtra(key, -1L)?.takeIf { it > 0 }

    /** A non-negative count or instant (an index, a millisecond time). */
    fun nonNegativeLong(intent: Intent?, key: String): Long? =
        intent?.getLongExtra(key, -1L)?.takeIf { it >= 0 }

    fun nonNegativeInt(intent: Intent?, key: String): Int? =
        intent?.getIntExtra(key, -1)?.takeIf { it >= 0 }

    /** A server or client id: a UUID and nothing else. */
    fun uuid(intent: Intent?, key: String): String? =
        intent?.getStringExtra(key)?.takeIf { UUID.matches(it) }

    /** A date saved as its epoch day, within years any alarm could be for. */
    fun epochDay(intent: Intent?, key: String): LocalDate? =
        intent?.takeIf { it.hasExtra(key) }?.getLongExtra(key, -1L)?.takeIf { it in 0..MAX_EPOCH_DAY }?.let(LocalDate::ofEpochDay)

    /** Text for a notification or prompt: control characters dropped, cut to [MAX_LABEL], or the fallback. */
    fun label(intent: Intent?, key: String, fallback: String): String =
        intent?.getStringExtra(key)?.let(::cleanLabel)?.takeIf { it.isNotBlank() } ?: fallback

    internal fun cleanLabel(text: String): String =
        text.filterNot { Character.isISOControl(it) }.trim().take(MAX_LABEL)
}
