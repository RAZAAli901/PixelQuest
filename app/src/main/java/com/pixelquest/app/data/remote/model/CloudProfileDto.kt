package com.pixelquest.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for public.profiles table in Supabase Postgrest.
 */
@Serializable
data class CloudProfileDto(
    @SerialName("id")
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("current_streak")
    val currentStreak: Int = 0,
    @SerialName("longest_streak")
    val longestStreak: Int = 0,
    @SerialName("level")
    val level: Int = 1,
    @SerialName("total_xp")
    val totalXp: Int = 0,
    @SerialName("leaderboard_opt_in")
    val leaderboardOptIn: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
