package com.pixelquest.app.darood.sync

import android.content.Context
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.darood.data.local.DaroodDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Whose Darood Party cache is on the phone: the account it was filled for. */
interface DaroodCacheOwner {
    fun owner(): String?
    fun setOwner(accountId: String?)
}

@Singleton
class PrefsDaroodCacheOwner @Inject constructor(@ApplicationContext context: Context) : DaroodCacheOwner {
    private val prefs = context.getSharedPreferences("pixelquest_darood", Context.MODE_PRIVATE)
    override fun owner(): String? = prefs.getString(KEY, null)
    override fun setOwner(accountId: String?) {
        prefs.edit().putString(KEY, accountId).apply()
    }

    private companion object {
        const val KEY = "cache_account"
    }
}

/**
 * Darood Party's data on the phone belongs to the account that's signed in (docs/darood-design.md).
 * Signing out clears it: parties, members, entries, and anything still waiting to be sent (the
 * sign-out tries to send that first). So does a different account signing in, even between
 * launches, since the cache remembers whose it is. It's never part of the game's backups.
 *
 * At each launch and sign-in it then brings the account's parties from the server, so a waiting
 * invitation shows (the side menu's dot) before the party list is ever opened.
 */
@Singleton
class DaroodAccountCleanup @Inject constructor(
    private val authRepository: AuthRepository,
    private val dao: DaroodDao,
    private val owner: DaroodCacheOwner,
    private val parties: com.pixelquest.app.darood.data.DaroodPartyRepository? = null
) {

    fun start(scope: CoroutineScope): Job = scope.launch {
        authRepository.currentUser.map { it?.id }.distinctUntilChanged().collect { onAccount(it) }
    }

    internal suspend fun onAccount(accountId: String?) {
        if (accountId == null || owner.owner() != accountId) {
            dao.clearAll()
            owner.setOwner(accountId)
        }
        // After any clearing, so the two can't race: the account's parties and invitations.
        if (accountId != null) parties?.refreshParties()
    }
}
