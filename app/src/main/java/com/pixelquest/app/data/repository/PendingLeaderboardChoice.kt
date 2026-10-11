package com.pixelquest.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A leaderboard choice made on this phone (join under a name, or leave) that the server hasn't
 * confirmed yet, kept with the account it belongs to.
 *
 * Joining or leaving belongs to the account, not the phone. A choice still waiting to be sent is the
 * newest and wins; otherwise the server's row holds the account's choice, and the phone follows it
 * (ProfileSyncWorker). Before, a phone that was off the board re-sent "off" on every sync and
 * one that was on re-sent "on", so with one account on two phones whichever synced last decided.
 */
@Singleton
class PendingLeaderboardChoice internal constructor(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    data class Choice(val optIn: Boolean, val displayName: String?)

    /** [accountId]'s choice not yet confirmed by the server, or null when there is none. */
    fun pendingFor(accountId: String): Choice? {
        if (prefs.getString(KEY_ACCOUNT, null) != accountId || !prefs.contains(KEY_OPT_IN)) return null
        return Choice(prefs.getBoolean(KEY_OPT_IN, false), prefs.getString(KEY_NAME, null))
    }

    /** [accountId]'s pending choice now and whenever it changes (chosen here, or confirmed by a sync). */
    fun pendingChanges(accountId: String): Flow<Choice?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(pendingFor(accountId)) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(pendingFor(accountId))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** The player chose on this phone: it stays pending until the server confirms it. */
    fun choose(accountId: String, optIn: Boolean, displayName: String?) {
        prefs.edit()
            .putBoolean(KEY_TRACKING, true)
            .putString(KEY_ACCOUNT, accountId)
            .putBoolean(KEY_OPT_IN, optIn)
            .putString(KEY_NAME, displayName)
            .apply()
    }

    /** The server now holds [optIn] for [accountId]: a matching pending choice is done. */
    fun confirmed(accountId: String, optIn: Boolean) {
        if (pendingFor(accountId)?.optIn == optIn) {
            prefs.edit().remove(KEY_ACCOUNT).remove(KEY_OPT_IN).remove(KEY_NAME).apply()
        }
    }

    /** An account new to this phone: it follows its own row on the server from now on. */
    fun startTracking() {
        prefs.edit().putBoolean(KEY_TRACKING, true).apply()
    }

    /**
     * First run after updating from a version that didn't keep pending choices. A phone that is off
     * the board treats that as its own unsent choice, so following the server can't put back a
     * player who had left (an opt-out that never reached the server would otherwise be undone).
     */
    fun startTrackingAfterUpdate(accountId: String, onTheBoardHere: Boolean) {
        if (prefs.getBoolean(KEY_TRACKING, false)) return
        if (onTheBoardHere) startTracking() else choose(accountId, optIn = false, displayName = null)
    }

    companion object {
        const val PREFS_NAME = "pixelquest_leaderboard_choice"
        private const val KEY_TRACKING = "tracking"
        private const val KEY_ACCOUNT = "pending_account"
        private const val KEY_OPT_IN = "pending_opt_in"
        private const val KEY_NAME = "pending_display_name"
    }
}
