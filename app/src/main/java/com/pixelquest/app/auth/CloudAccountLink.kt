package com.pixelquest.app.auth

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Links the signed-in account to this device's profile, and keeps the choices that belong to a
 * person with the account that made them. The leaderboard opt-in, the public display name and the
 * AI Coach consent are stored on the device, so when a different account signs in on the same phone
 * they're reset and the new person chooses for themselves. Before, the next account was put on the
 * public leaderboard under the previous person's name without ever opting in. Signing out and back
 * in as the same account keeps everything.
 *
 * An account that is new to this phone (a returning player after someone else, a reinstall, a second
 * phone) gets its leaderboard choice and name back from its own row on the server. Found on the
 * emulator: such an account was shown as off the board while the board still listed it, and the next
 * sync took it off without the player asking.
 */
@Singleton
class CloudAccountLink internal constructor(
    private val prefs: SharedPreferences,
    private val userProfileRepository: UserProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val cloudProfiles: CloudProfileRepository? = null
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        userProfileRepository: UserProfileRepository,
        settingsRepository: SettingsRepository,
        cloudProfiles: CloudProfileRepository
    ) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE), userProfileRepository, settingsRepository, cloudProfiles)

    /**
     * Call whenever an account is signed in (a new sign-in or a restored session). Returns true when
     * it's a different account from the last one on this device, and the choices were reset.
     */
    suspend fun onSignedIn(accountId: String): Boolean {
        val previous = prefs.getString(KEY_LAST_ACCOUNT, null)
        val linked = userProfileRepository.getProfile().first()?.supabaseUserId
        prefs.edit().putString(KEY_LAST_ACCOUNT, accountId).apply()
        // A profile linked before this device remembered the last account counts as the same one.
        if (previous == accountId || (previous == null && linked == accountId)) {
            userProfileRepository.updateSupabaseUserId(accountId)
            return false
        }
        val otherAccount = previous != null || (linked != null && linked != accountId)

        // Read before the account is linked: a sync in between would send "not opted in" for it.
        // Unreadable (offline), a different account still starts with everything off.
        val own = (cloudProfiles?.fetchCloudProfile(accountId) as? SupabaseResult.Success)?.data
        when {
            own != null -> userProfileRepository.updateLeaderboardSettings(
                optIn = own.leaderboardOptIn,
                displayName = own.displayName.ifBlank { null }
            )
            otherAccount -> userProfileRepository.updateLeaderboardSettings(optIn = false, displayName = null)
        }
        // The AI Coach consent isn't kept on the server: the person turns it on again.
        if (otherAccount) settingsRepository.setAiInsightsEnabled(false)
        userProfileRepository.updateSupabaseUserId(accountId)
        return otherAccount
    }

    companion object {
        const val PREFS_NAME = "pixelquest_cloud_account"
        private const val KEY_LAST_ACCOUNT = "last_signed_in_account"
    }
}
