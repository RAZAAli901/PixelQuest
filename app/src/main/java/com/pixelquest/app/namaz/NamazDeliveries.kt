package com.pixelquest.app.namaz

import android.content.Context
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which reminders and nudges have already fired, so a reschedule never arms one again. Without it,
 * moving the reminder time from 15 minutes before to the start, or a time-zone change that moves
 * the times later, armed a second reminder for a prayer already reminded.
 */
interface NamazDeliveries {
    fun wasDelivered(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer): Boolean

    fun markDelivered(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer)
}

/** [NamazDeliveries] in SharedPreferences, kept for the last few days only. */
@Singleton
class PrefsNamazDeliveries @Inject constructor(
    @ApplicationContext private val context: Context
) : NamazDeliveries {

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override fun wasDelivered(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer): Boolean =
        key(kind, date, prayer) in prefs.getStringSet(KEY, emptySet()).orEmpty()

    @Synchronized
    override fun markDelivered(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer) {
        // Older than the window ever reaches back: dropped, so the set stays small.
        val oldest = date.minusDays(KEEP_DAYS)
        val kept = prefs.getStringSet(KEY, emptySet()).orEmpty()
            .filter { entry -> entry.split('|').getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.isBefore(oldest) == false }
            .toSet()
        prefs.edit().putStringSet(KEY, kept + key(kind, date, prayer)).apply()
    }

    private fun key(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer) = "${kind.name}|$date|${prayer.name}"

    private companion object {
        const val PREFS = "pixelquest_namaz_delivered"
        const val KEY = "delivered"
        const val KEEP_DAYS = 4L
    }
}
