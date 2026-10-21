package com.pixelquest.app.study.alarm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether study reminders alert on this phone (docs/study-reminders.md, Channel): all of them, on
 * unless the player turns them off, and each party's, unless it's muted. Muted, a party keeps its
 * reminders (they still show on This week) but posts none. Nothing here is shared.
 */
interface StudyReminderMutes {
    val alertsOn: StateFlow<Boolean>
    val muted: StateFlow<Set<String>>

    fun setAlertsOn(on: Boolean)
    fun setMuted(partyId: String, muted: Boolean)

    fun alerts(partyId: String): Boolean = alertsOn.value && partyId !in muted.value
}

/** Kept in the study reminders' preferences. */
@Singleton
class PrefsStudyReminderMutes @Inject constructor(
    @ApplicationContext context: Context
) : StudyReminderMutes {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val on = MutableStateFlow(prefs.getBoolean(KEY_ON, true))
    private val parties = MutableStateFlow(prefs.getStringSet(KEY_MUTED, emptySet()).orEmpty().toSet())

    override val alertsOn: StateFlow<Boolean> = on.asStateFlow()
    override val muted: StateFlow<Set<String>> = parties.asStateFlow()

    override fun setAlertsOn(on: Boolean) {
        prefs.edit().putBoolean(KEY_ON, on).apply()
        this.on.value = on
    }

    @Synchronized
    override fun setMuted(partyId: String, muted: Boolean) {
        val next = if (muted) parties.value + partyId else parties.value - partyId
        prefs.edit().putStringSet(KEY_MUTED, next).apply()
        parties.value = next
    }

    private companion object {
        const val PREFS = "pixelquest_study_reminder_mutes"
        const val KEY_ON = "alerts_on"
        const val KEY_MUTED = "muted_parties"
    }
}

/** In memory, for tests. */
class MemoryStudyReminderMutes(on: Boolean = true, muted: Set<String> = emptySet()) : StudyReminderMutes {
    private val onFlow = MutableStateFlow(on)
    private val mutedFlow = MutableStateFlow(muted)
    override val alertsOn: StateFlow<Boolean> = onFlow
    override val muted: StateFlow<Set<String>> = mutedFlow
    override fun setAlertsOn(on: Boolean) {
        onFlow.value = on
    }

    override fun setMuted(partyId: String, muted: Boolean) {
        mutedFlow.value = if (muted) mutedFlow.value + partyId else mutedFlow.value - partyId
    }
}
