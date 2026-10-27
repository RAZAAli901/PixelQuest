package com.pixelquest.app.study.data

import android.content.Context
import com.pixelquest.app.study.domain.StudyRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Current operating state of the study timer. */
enum class TimerState { RUNNING, PAUSED, STOPPED }

/**
 * A study timer running or paused for [partyId] since [startedAt] (epoch millis).
 * Tracks pause moments and cumulative paused duration so elapsed study time is exact.
 */
data class RunningTimer(
    val partyId: String,
    val startedAt: Long,
    val partyName: String? = null,
    val pausedAt: Long? = null,
    val accumulatedPauseMs: Long = 0L,
) {
    val state: TimerState
        get() = if (pausedAt != null) TimerState.PAUSED else TimerState.RUNNING

    val isPaused: Boolean get() = pausedAt != null

    /** Effective elapsed active study duration in milliseconds at given moment [now]. */
    fun elapsedMillisAt(now: Long): Long = if (isPaused) {
        val pauseMoment = pausedAt ?: now
        (pauseMoment - startedAt - accumulatedPauseMs).coerceAtLeast(0L)
    } else {
        (now - startedAt - accumulatedPauseMs).coerceAtLeast(0L)
    }

    /**
     * Chronometer base timestamp for NotificationCompat.Builder.setWhen().
     * Elapsed realtime is used so system chronometer matches elapsed active millis.
     */
    fun chronometerBase(currentRealtime: Long, now: Long): Long {
        val elapsed = elapsedMillisAt(now)
        return currentRealtime - elapsed
    }

    /** The whole minutes it has run at [now]. */
    fun minutesAt(now: Long): Int = (elapsedMillisAt(now) / 60_000L).toInt()

    /** What stopping it at [now] logs: its minutes up to 240, or null under 5, too short to count. */
    fun loggedAt(now: Long): Int? = minutesAt(now).takeIf { it >= StudyRules.MINUTES_MIN }?.coerceAtMost(StudyRules.MINUTES_MAX)
}

/**
 * The study timer (docs/study-party.md, docs/study-timer.md): one at a time across all circles.
 * State is persisted so it survives process death, app kill, and device reboot.
 */
interface StudyTimerStore {
    val running: StateFlow<RunningTimer?>
    fun start(partyId: String, at: Long, partyName: String? = null)
    fun pause(at: Long = System.currentTimeMillis())
    fun resume(at: Long = System.currentTimeMillis())
    fun clear()
}

@Singleton
class PrefsStudyTimerStore @Inject constructor(@ApplicationContext context: Context) : StudyTimerStore {
    private val prefs = context.getSharedPreferences("pixelquest_study", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    override val running: StateFlow<RunningTimer?> = state.asStateFlow()

    private fun read(): RunningTimer? {
        val party = prefs.getString(KEY_PARTY, null) ?: return null
        val startedAt = prefs.getLong(KEY_STARTED, 0L)
        val partyName = prefs.getString(KEY_PARTY_NAME, null)
        val pausedAt = if (prefs.contains(KEY_PAUSED)) prefs.getLong(KEY_PAUSED, 0L) else null
        val accumulated = prefs.getLong(KEY_ACCUMULATED, 0L)
        return RunningTimer(party, startedAt, partyName, pausedAt, accumulated)
    }

    override fun start(partyId: String, at: Long, partyName: String?) {
        val editor = prefs.edit()
            .putString(KEY_PARTY, partyId)
            .putLong(KEY_STARTED, at)
            .remove(KEY_PAUSED)
            .putLong(KEY_ACCUMULATED, 0L)
        if (partyName != null) {
            editor.putString(KEY_PARTY_NAME, partyName)
        } else {
            editor.remove(KEY_PARTY_NAME)
        }
        editor.apply()
        state.value = RunningTimer(partyId, at, partyName, null, 0L)
    }

    override fun pause(at: Long) {
        val current = state.value ?: return
        if (current.isPaused) return
        prefs.edit().putLong(KEY_PAUSED, at).apply()
        state.value = current.copy(pausedAt = at)
    }

    override fun resume(at: Long) {
        val current = state.value ?: return
        val pausedAt = current.pausedAt ?: return
        val pauseDelta = (at - pausedAt).coerceAtLeast(0L)
        val newAccumulated = current.accumulatedPauseMs + pauseDelta
        prefs.edit()
            .remove(KEY_PAUSED)
            .putLong(KEY_ACCUMULATED, newAccumulated)
            .apply()
        state.value = current.copy(pausedAt = null, accumulatedPauseMs = newAccumulated)
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_PARTY)
            .remove(KEY_STARTED)
            .remove(KEY_PARTY_NAME)
            .remove(KEY_PAUSED)
            .remove(KEY_ACCUMULATED)
            .apply()
        state.value = null
    }

    private companion object {
        const val KEY_PARTY = "timer_party"
        const val KEY_STARTED = "timer_started_at"
        const val KEY_PARTY_NAME = "timer_party_name"
        const val KEY_PAUSED = "timer_paused_at"
        const val KEY_ACCUMULATED = "timer_accumulated_pause"
    }
}

/** A timer kept in memory, for tests and previews. */
class MemoryStudyTimerStore : StudyTimerStore {
    private val state = MutableStateFlow<RunningTimer?>(null)
    override val running: StateFlow<RunningTimer?> = state.asStateFlow()

    override fun start(partyId: String, at: Long, partyName: String?) {
        state.value = RunningTimer(partyId, at, partyName, null, 0L)
    }

    override fun pause(at: Long) {
        val current = state.value ?: return
        if (current.isPaused) return
        state.value = current.copy(pausedAt = at)
    }

    override fun resume(at: Long) {
        val current = state.value ?: return
        val pausedAt = current.pausedAt ?: return
        val pauseDelta = (at - pausedAt).coerceAtLeast(0L)
        state.value = current.copy(pausedAt = null, accumulatedPauseMs = current.accumulatedPauseMs + pauseDelta)
    }

    override fun clear() {
        state.value = null
    }
}
