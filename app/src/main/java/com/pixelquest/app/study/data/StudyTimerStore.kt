package com.pixelquest.app.study.data

import android.content.Context
import com.pixelquest.app.study.domain.StudyRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A study timer running for [partyId] since [startedAt] (epoch millis). */
data class RunningTimer(val partyId: String, val startedAt: Long) {
    /** The whole minutes it has run at [now]. */
    fun minutesAt(now: Long): Int = ((now - startedAt).coerceAtLeast(0) / 60_000L).toInt()

    /** What stopping it at [now] logs: its minutes up to 240, or null under 5, too short to count. */
    fun loggedAt(now: Long): Int? = minutesAt(now).takeIf { it >= StudyRules.MINUTES_MIN }?.coerceAtMost(StudyRules.MINUTES_MAX)
}

/**
 * The study timer (docs/study-party.md, Study time): one at a time, for one party. Only when it
 * started is kept, so it keeps running while the app is closed, and nothing ticks in the background.
 */
interface StudyTimerStore {
    val running: StateFlow<RunningTimer?>
    fun start(partyId: String, at: Long)
    fun clear()
}

@Singleton
class PrefsStudyTimerStore @Inject constructor(@ApplicationContext context: Context) : StudyTimerStore {
    private val prefs = context.getSharedPreferences("pixelquest_study", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    override val running: StateFlow<RunningTimer?> = state.asStateFlow()

    private fun read(): RunningTimer? {
        val party = prefs.getString(KEY_PARTY, null) ?: return null
        return RunningTimer(party, prefs.getLong(KEY_STARTED, 0L))
    }

    override fun start(partyId: String, at: Long) {
        prefs.edit().putString(KEY_PARTY, partyId).putLong(KEY_STARTED, at).apply()
        state.value = RunningTimer(partyId, at)
    }

    override fun clear() {
        prefs.edit().remove(KEY_PARTY).remove(KEY_STARTED).apply()
        state.value = null
    }

    private companion object {
        const val KEY_PARTY = "timer_party"
        const val KEY_STARTED = "timer_started_at"
    }
}

/** A timer kept in memory, for tests and previews. */
class MemoryStudyTimerStore : StudyTimerStore {
    private val state = MutableStateFlow<RunningTimer?>(null)
    override val running: StateFlow<RunningTimer?> = state.asStateFlow()
    override fun start(partyId: String, at: Long) {
        state.value = RunningTimer(partyId, at)
    }

    override fun clear() {
        state.value = null
    }
}
