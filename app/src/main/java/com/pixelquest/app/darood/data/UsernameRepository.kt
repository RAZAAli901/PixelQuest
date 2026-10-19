package com.pixelquest.app.darood.data

import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in account's Darood Party username (docs/darood-design.md, Usernames). The server
 * keeps usernames in their own table, readable only by their owner, and answers questions about
 * others' through functions.
 */
interface UsernameRepository {
    /** Whether [name] is free for this account; its own current name counts as free. Signed in only. */
    suspend fun isUsernameAvailable(name: Username): SupabaseResult<Boolean>
}

@Singleton
class SupabaseUsernameRepository @Inject constructor(
    private val postgrest: Postgrest
) : UsernameRepository {

    override suspend fun isUsernameAvailable(name: Username): SupabaseResult<Boolean> = safeSupabaseCall {
        postgrest.rpc("is_username_available", buildJsonObject { put("name", name.value) }).decodeAs<Boolean>()
    }
}
