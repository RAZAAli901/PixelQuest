package com.pixelquest.app.testing

import com.pixelquest.app.darood.data.OwnUsername
import com.pixelquest.app.darood.data.SetUsernameResult
import com.pixelquest.app.darood.data.UsernameRepository
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.data.remote.SupabaseResult
import java.io.IOException
import java.time.Instant

/**
 * Usernames held in memory, by the server's rules: unique, one change every 30 days, the
 * account's own name counting as free. [taken] are other accounts' names; [offline] makes every
 * call fail as a network error.
 */
class FakeUsernameRepository(
    var mine: OwnUsername? = null,
    val taken: MutableSet<String> = mutableSetOf(),
    var now: Instant = Instant.parse("2026-10-19T08:00:00Z")
) : UsernameRepository {
    var offline = false
    val availabilityChecks = mutableListOf<String>()
    val saves = mutableListOf<String>()

    private fun <T> answer(block: () -> T): SupabaseResult<T> =
        if (offline) SupabaseResult.NetworkError(IOException("offline"), "offline") else SupabaseResult.Success(block())

    override suspend fun fetchMyUsername(): SupabaseResult<OwnUsername?> = answer { mine }

    override suspend fun isUsernameAvailable(name: Username): SupabaseResult<Boolean> = answer {
        availabilityChecks += name.value
        name.value !in taken
    }

    override suspend fun setUsername(name: Username): SupabaseResult<SetUsernameResult> = answer {
        saves += name.value
        val held = mine
        when {
            held?.username == name -> SetUsernameResult.Saved(name, held.changeableAt)
            name.value in taken -> SetUsernameResult.Taken
            held != null && held.changeableAt.isAfter(now) -> SetUsernameResult.TooSoon(held.username, held.changeableAt)
            else -> SetUsernameResult.Saved(name, now.plus(UsernameRepository.CHANGE_WAIT)).also {
                mine = OwnUsername(name, it.changeableAt)
            }
        }
    }
}

/** The accounts asked for a username, in memory. */
class FakeUsernamePromptStore(val prompted: MutableSet<String> = mutableSetOf()) : com.pixelquest.app.darood.data.UsernamePromptStore {
    override fun wasPrompted(accountId: String): Boolean = accountId in prompted
    override fun markPrompted(accountId: String) {
        prompted += accountId
    }
}

/** The username the phone keeps, in memory. */
class FakeUsernameCache(initial: com.pixelquest.app.darood.data.CachedUsername? = null) : com.pixelquest.app.darood.data.UsernameCache {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(initial)
    override val cached: kotlinx.coroutines.flow.Flow<com.pixelquest.app.darood.data.CachedUsername?> = state
    val value: com.pixelquest.app.darood.data.CachedUsername? get() = state.value
    override fun save(accountId: String, username: Username) {
        state.value = com.pixelquest.app.darood.data.CachedUsername(accountId, username)
    }
}
