package com.pixelquest.app.darood.data

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.darood.domain.Username
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** An account's username as the server last gave it. */
data class CachedUsername(val accountId: String, val username: Username)

/**
 * The username last seen from the server, kept on the phone so the profile and the side menu can
 * show it offline. One account at a time: the one last signed in.
 */
interface UsernameCache {
    val cached: Flow<CachedUsername?>
    fun save(accountId: String, username: Username)
}

@Singleton
class PrefsUsernameCache @Inject constructor(
    @ApplicationContext context: Context
) : UsernameCache {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(): CachedUsername? {
        val account = prefs.getString(KEY_ACCOUNT, null) ?: return null
        val name = prefs.getString(KEY_USERNAME, null)?.let(Username::parse) ?: return null
        return CachedUsername(account, name)
    }

    override val cached: Flow<CachedUsername?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ACCOUNT || key == KEY_USERNAME) trySend(read())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override fun save(accountId: String, username: Username) {
        prefs.edit().putString(KEY_ACCOUNT, accountId).putString(KEY_USERNAME, username.value).apply()
    }

    private companion object {
        const val PREFS = "pixelquest_darood"
        const val KEY_ACCOUNT = "username_account"
        const val KEY_USERNAME = "username"
    }
}

/** The signed-in account's username: null when signed out, or when it has none the phone knows of. */
@Singleton
class MyUsername @Inject constructor(
    authRepository: AuthRepository,
    cache: UsernameCache
) {
    val username: Flow<Username?> = combine(authRepository.currentUser, cache.cached) { user, cached ->
        // Another account's name, from before a switch, is never shown as this one's.
        cached?.takeIf { user != null && it.accountId == user.id }?.username
    }.distinctUntilChanged()
}
