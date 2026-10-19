package com.pixelquest.app.darood.data

import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** What happened to a [UsernameRepository.setUsername]. */
sealed interface SetUsernameResult {
    /** [username] is this account's now; it can change again from [changeableAt]. */
    data class Saved(val username: Username, val changeableAt: Instant) : SetUsernameResult

    /** Another account has it, in some case. */
    data object Taken : SetUsernameResult

    /** It breaks a rule the app checks too (form, reserved word): only a stale or modified app gets this. */
    data object Invalid : SetUsernameResult

    /** The account changed its name less than 30 days ago: it's still [current] until [changeableAt]. */
    data class TooSoon(val current: Username, val changeableAt: Instant) : SetUsernameResult
}

/**
 * The signed-in account's Darood Party username (docs/darood-design.md, Usernames). The server
 * keeps usernames in their own table, readable only by their owner, and answers questions about
 * others' through functions.
 */
interface UsernameRepository {
    /** Whether [name] is free for this account; its own current name counts as free. Signed in only. */
    suspend fun isUsernameAvailable(name: Username): SupabaseResult<Boolean>

    /** Claims [name] for this account, or changes to it: once every 30 days, which the server enforces. */
    suspend fun setUsername(name: Username): SupabaseResult<SetUsernameResult>
}

@Singleton
class SupabaseUsernameRepository @Inject constructor(
    private val postgrest: Postgrest
) : UsernameRepository {

    override suspend fun isUsernameAvailable(name: Username): SupabaseResult<Boolean> = safeSupabaseCall {
        postgrest.rpc("is_username_available", buildJsonObject { put("name", name.value) }).decodeAs<Boolean>()
    }

    override suspend fun setUsername(name: Username): SupabaseResult<SetUsernameResult> = safeSupabaseCall {
        postgrest.rpc("set_username", buildJsonObject { put("name", name.value) }).decodeAs<SetUsernameReply>().toResult()
    }
}

/** set_username's answer (supabase/migrations/20261019000003_set_username.sql). */
@Serializable
internal data class SetUsernameReply(
    val status: String,
    val username: String? = null,
    @SerialName("changeable_at") val changeableAt: String? = null
) {
    fun toResult(): SetUsernameResult {
        val held = username?.let(Username::parse)
        val after = changeableAt?.let { OffsetDateTime.parse(it).toInstant() }
        return when {
            status == "ok" && held != null && after != null -> SetUsernameResult.Saved(held, after)
            status == "too_soon" && held != null && after != null -> SetUsernameResult.TooSoon(held, after)
            status == "taken" -> SetUsernameResult.Taken
            else -> SetUsernameResult.Invalid
        }
    }
}
