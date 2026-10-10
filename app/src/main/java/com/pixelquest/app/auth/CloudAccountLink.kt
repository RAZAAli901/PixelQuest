package com.pixelquest.app.auth

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Links the signed-in account to this device's profile, and keeps the choices that belong to a
 * person with the account that made them. The leaderboard opt-in, the public display name and the
 * AI Coach consent are stored on the device, so when a different account signs in on the same phone
 * they're reset and the new person chooses for themselves. Before, the next account was put on the
 * public leaderboard under the previous person's name without ever opting in. Signing out and back
 * in as the same account keeps everything.
 */
@Singleton
class CloudAccountLink internal constructor(
    private val prefs: SharedPreferences,
    private val userProfileRepository: UserProfileRepository,
    private val settingsRepository: SettingsRepository
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        userProfileRepository: UserProfileRepository,
        settingsRepository: SettingsRepository
    ) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE), userProfileRepository, settingsRepository)

    /**
     * Call whenever an account is signed in (a new sign-in or a restored session). Returns true when
     * it's a different account from the last one on this device, and the choices were reset.
     */
    suspend fun onSignedIn(accountId: String): Boolean {
        val previous = prefs.getString(KEY_LAST_ACCOUNT, null)
        prefs.edit().putString(KEY_LAST_ACCOUNT, accountId).apply()
        userProfileRepository.updateSupabaseUserId(accountId)
        if (previous == null || previous == accountId) return false
        userProfileRepository.updateLeaderboardSettings(optIn = false, displayName = null)
        settingsRepository.setAiInsightsEnabled(false)
        return true
    }

    companion object {
        const val PREFS_NAME = "pixelquest_cloud_account"
        private const val KEY_LAST_ACCOUNT = "last_signed_in_account"
    }
}
