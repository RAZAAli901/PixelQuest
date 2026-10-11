package com.pixelquest.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RESET ALL PROGRESS on a phone whose account is on the leaderboard, not yet sent to the board.
 *
 * The sync never sends numbers lower than the board's (another phone may be further on), so after a
 * reset the board kept the old streak and level until the player passed them again. The fresh
 * numbers are now sent once, past that guard, by ProfileSyncWorker; this stays set until the server
 * has them, so a reset made offline arrives later.
 */
@Singleton
class PendingProgressReset internal constructor(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    fun isPendingFor(accountId: String): Boolean = prefs.getString(KEY_ACCOUNT, null) == accountId

    fun mark(accountId: String) = prefs.edit().putString(KEY_ACCOUNT, accountId).apply()

    fun sent(accountId: String) {
        if (isPendingFor(accountId)) prefs.edit().remove(KEY_ACCOUNT).apply()
    }

    companion object {
        const val PREFS_NAME = "pixelquest_progress_reset"
        private const val KEY_ACCOUNT = "reset_account"
    }
}
