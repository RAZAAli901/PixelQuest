package com.pixelquest.app.darood.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the weekly ranking's notification comes (docs/darood-weekly.md, Turning it off): on for
 * every party unless the player turns it off everywhere ([allOn]) or for one party ([muted]), on
 * this phone. The ranking itself, in the chat and on its screen, is there either way.
 */
interface WeekRankingMutes {
    val allOn: StateFlow<Boolean>
    val muted: StateFlow<Set<String>>

    fun setAllOn(on: Boolean)
    fun setMuted(partyId: String, muted: Boolean)

    /** No notification for [partyId]'s weeks. */
    fun silent(partyId: String): Boolean = !allOn.value || partyId in muted.value
}

@Singleton
class PrefsWeekRankingMutes @Inject constructor(@ApplicationContext context: Context) : WeekRankingMutes {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _allOn = MutableStateFlow(prefs.getBoolean(KEY_ALL, true))
    private val _muted = MutableStateFlow(prefs.getStringSet(KEY_MUTED, emptySet()).orEmpty().toSet())
    override val allOn: StateFlow<Boolean> = _allOn.asStateFlow()
    override val muted: StateFlow<Set<String>> = _muted.asStateFlow()

    override fun setAllOn(on: Boolean) {
        prefs.edit().putBoolean(KEY_ALL, on).apply()
        _allOn.value = on
    }

    @Synchronized
    override fun setMuted(partyId: String, muted: Boolean) {
        val next = if (muted) _muted.value + partyId else _muted.value - partyId
        prefs.edit().putStringSet(KEY_MUTED, next).apply()
        _muted.value = next
    }

    private companion object {
        const val PREFS = "pixelquest_darood_week"
        const val KEY_ALL = "ranking_on"
        const val KEY_MUTED = "ranking_muted"
    }
}
