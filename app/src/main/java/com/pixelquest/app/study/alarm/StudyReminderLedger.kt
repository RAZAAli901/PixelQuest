package com.pixelquest.app.study.alarm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What this phone has armed for study reminders, and which alerts have fired: so a reminder gone is
 * cancelled, and an alert is never fired twice (a reschedule, or a restart's catch-up, after it came).
 */
interface StudyReminderLedger {
    fun armed(): Set<String>
    fun setArmed(slots: Set<String>)

    fun wasFired(key: String): Boolean

    /** Records [key] as fired at [atMillis]; those older than [keepSinceMillis] are forgotten. */
    fun markFired(key: String, atMillis: Long, keepSinceMillis: Long)
}

/** Kept in the study feature's preferences. */
@Singleton
class PrefsStudyReminderLedger @Inject constructor(
    @ApplicationContext context: Context
) : StudyReminderLedger {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun armed(): Set<String> = prefs.getStringSet(KEY_ARMED, emptySet()).orEmpty().toSet()

    override fun setArmed(slots: Set<String>) {
        prefs.edit().putStringSet(KEY_ARMED, slots).apply()
    }

    private fun fired(): Map<String, Long> = prefs.getStringSet(KEY_FIRED, emptySet()).orEmpty()
        .mapNotNull { entry -> entry.substringAfterLast('@').toLongOrNull()?.let { entry.substringBeforeLast('@') to it } }
        .toMap()

    override fun wasFired(key: String): Boolean = key in fired()

    @Synchronized
    override fun markFired(key: String, atMillis: Long, keepSinceMillis: Long) {
        val kept = fired().filterValues { it >= keepSinceMillis } + (key to atMillis)
        prefs.edit().putStringSet(KEY_FIRED, kept.map { (k, at) -> "$k@$at" }.toSet()).apply()
    }

    private companion object {
        const val PREFS = "pixelquest_study_reminders"
        const val KEY_ARMED = "armed"
        const val KEY_FIRED = "fired"
    }
}

/** In memory, for tests. */
class MemoryStudyReminderLedger : StudyReminderLedger {
    private var armed = emptySet<String>()
    private val fired = mutableMapOf<String, Long>()

    override fun armed() = armed
    override fun setArmed(slots: Set<String>) {
        armed = slots
    }

    override fun wasFired(key: String) = key in fired

    override fun markFired(key: String, atMillis: Long, keepSinceMillis: Long) {
        fired.entries.removeIf { it.value < keepSinceMillis }
        fired[key] = atMillis
    }
}
