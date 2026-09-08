package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

enum class LeaderboardSortMode {
    STREAK,
    LEVEL
}

data class UserLeaderboardRank(
    val rank: Int,
    val profile: CloudProfileDto
)

interface LeaderboardRepository {
    suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByStreak(limit: Long = 20, offset: Long = 0): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByLevel(limit: Long = 20, offset: Long = 0): SupabaseResult<List<CloudProfileDto>>
    suspend fun getCurrentUserRank(
        sortMode: LeaderboardSortMode,
        userId: String? = null
    ): SupabaseResult<UserLeaderboardRank?>
}

@Singleton
open class LeaderboardRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth? = null
) : LeaderboardRepository {

    override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
                order("current_streak", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                order("longest_streak", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                if (offset > 0) {
                    range(from = offset, to = offset + limit - 1)
                } else {
                    limit(limit)
                }
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
                order("level", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                order("total_xp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                if (offset > 0) {
                    range(from = offset, to = offset + limit - 1)
                } else {
                    limit(limit)
                }
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getCurrentUserRank(
        sortMode: LeaderboardSortMode,
        userId: String?
    ): SupabaseResult<UserLeaderboardRank?> {
        return safeSupabaseCall {
            val targetId = userId ?: auth?.currentUserOrNull()?.id ?: return@safeSupabaseCall null

            // 1. Fetch user's profile
            val targetProfile = postgrest["profiles"].select {
                filter {
                    eq("id", targetId)
                }
            }.decodeList<CloudProfileDto>().firstOrNull()

            if (targetProfile == null || !targetProfile.leaderboardOptIn) {
                return@safeSupabaseCall null
            }

            // 2. Calculate 1-based rank by counting opted-in profiles ranked ahead
            val rank = when (sortMode) {
                LeaderboardSortMode.STREAK -> {
                    val higherStreaks = postgrest["profiles"].select {
                        filter {
                            eq("leaderboard_opt_in", true)
                            gt("current_streak", targetProfile.currentStreak)
                        }
                    }.decodeList<CloudProfileDto>().size

                    val sameStreakHigherLongest = postgrest["profiles"].select {
                        filter {
                            eq("leaderboard_opt_in", true)
                            eq("current_streak", targetProfile.currentStreak)
                            gt("longest_streak", targetProfile.longestStreak)
                        }
                    }.decodeList<CloudProfileDto>().size

                    higherStreaks + sameStreakHigherLongest + 1
                }
                LeaderboardSortMode.LEVEL -> {
                    val higherLevels = postgrest["profiles"].select {
                        filter {
                            eq("leaderboard_opt_in", true)
                            gt("level", targetProfile.level)
                        }
                    }.decodeList<CloudProfileDto>().size

                    val sameLevelHigherXp = postgrest["profiles"].select {
                        filter {
                            eq("leaderboard_opt_in", true)
                            eq("level", targetProfile.level)
                            gt("total_xp", targetProfile.totalXp)
                        }
                    }.decodeList<CloudProfileDto>().size

                    higherLevels + sameLevelHigherXp + 1
                }
            }

            UserLeaderboardRank(rank = rank, profile = targetProfile)
        }
    }
}
