package com.pixelquest.app.data.remote.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for public.profiles table in Supabase Postgrest.
 *
 * The numbers and the opt-in are always written, even when they equal their defaults. supabase-kt's
 * JSON leaves default values out, and an upsert keeps the columns it isn't sent. So a broken streak
 * (0) never reached the board, which went on showing the old one, and neither did RESET's level 1
 * and 0 XP.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CloudProfileDto(
    @SerialName("id")
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    @EncodeDefault
    @SerialName("current_streak")
    val currentStreak: Int = 0,
    @EncodeDefault
    @SerialName("longest_streak")
    val longestStreak: Int = 0,
    @EncodeDefault
    @SerialName("level")
    val level: Int = 1,
    @EncodeDefault
    @SerialName("total_xp")
    val totalXp: Int = 0,
    @EncodeDefault
    @SerialName("leaderboard_opt_in")
    val leaderboardOptIn: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
