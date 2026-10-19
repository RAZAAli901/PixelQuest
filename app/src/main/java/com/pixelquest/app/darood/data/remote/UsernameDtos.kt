package com.pixelquest.app.darood.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The account's own row in public.usernames (supabase/migrations/20261019000000_usernames.sql).
 * Row-level security returns only the caller's row, so this is never someone else's.
 */
@Serializable
data class UsernameDto(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("changed_at") val changedAt: String
)

/** set_username's answer (supabase/migrations/20261019000003_set_username.sql). */
@Serializable
data class SetUsernameReply(
    @SerialName("status") val status: String,
    @SerialName("username") val username: String? = null,
    @SerialName("changeable_at") val changeableAt: String? = null
)
