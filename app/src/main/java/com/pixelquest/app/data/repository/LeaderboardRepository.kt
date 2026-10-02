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

/** A profile with its 1-based position on the leaderboard. */
data class RankedProfile(
    val rank: Int,
    val profile: CloudProfileDto
)

/** The signed-in hero's rank plus the heroes just above and below them. */
data class PlayersAroundYou(
    val you: UserLeaderboardRank,
    val entries: List<RankedProfile>
)

/**
 * Which slice of the ordered leaderboard holds the [radius] heroes above and below [rank].
 * Ranks are 1-based positions; offsets are 0-based.
 */
object LeaderboardWindow {
    fun offset(rank: Int, radius: Int): Long = (rank - 1 - radius).coerceAtLeast(0).toLong()

    fun limit(rank: Int, radius: Int): Long = (rank - 1 - offset(rank, radius)) + 1L + radius

    fun ranked(entries: List<CloudProfileDto>, offset: Long): List<RankedProfile> =
        entries.mapIndexed { index, profile -> RankedProfile(rank = (offset + index + 1).toInt(), profile = profile) }
}

interface LeaderboardRepository {
    suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByStreak(limit: Long = 20, offset: Long = 0): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByLevel(limit: Long = 20, offset: Long = 0): SupabaseResult<List<CloudProfileDto>>
    suspend fun getCurrentUserRank(
        sortMode: LeaderboardSortMode,
        userId: String? = null
    ): SupabaseResult<UserLeaderboardRank?>
    suspend fun reportProfile(
        reportedProfileId: String,
        reason: String
    ): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    /**
     * The heroes ranked just above and below the signed-in user: their rank, then one page of the
     * same ordered list around it. Null when the user isn't on the leaderboard (not opted in, or
     * their profile hasn't synced yet).
     */
    suspend fun getPlayersAroundYou(
        sortMode: LeaderboardSortMode,
        userId: String? = null,
        radius: Int = 3
    ): SupabaseResult<PlayersAroundYou?> {
        val you = when (val rankResult = getCurrentUserRank(sortMode, userId)) {
            is SupabaseResult.Success -> rankResult.data ?: return SupabaseResult.Success(null)
            is SupabaseResult.NetworkError -> return rankResult
            is SupabaseResult.AuthError -> return rankResult
            is SupabaseResult.ServerError -> return rankResult
            is SupabaseResult.UnknownError -> return rankResult
        }
        val offset = LeaderboardWindow.offset(you.rank, radius)
        val limit = LeaderboardWindow.limit(you.rank, radius)
        val page = when (sortMode) {
            LeaderboardSortMode.STREAK -> getTopByStreak(limit = limit, offset = offset)
            LeaderboardSortMode.LEVEL -> getTopByLevel(limit = limit, offset = offset)
        }
        return when (page) {
            is SupabaseResult.Success -> SupabaseResult.Success(PlayersAroundYou(you, LeaderboardWindow.ranked(page.data, offset)))
            is SupabaseResult.NetworkError -> page
            is SupabaseResult.AuthError -> page
            is SupabaseResult.ServerError -> page
            is SupabaseResult.UnknownError -> page
        }
    }
}

@Singleton
open class LeaderboardRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth? = null
) : LeaderboardRepository {

    override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall(timeoutMs = LEADERBOARD_TIMEOUT_MS) {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall(timeoutMs = LEADERBOARD_TIMEOUT_MS) {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
                order("current_streak", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                order("longest_streak", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                order("id", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                if (offset > 0) {
                    range(from = offset, to = offset + limit - 1)
                } else {
                    limit(limit)
                }
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall(timeoutMs = LEADERBOARD_TIMEOUT_MS) {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
                order("level", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                order("total_xp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                order("id", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
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
        return safeSupabaseCall(timeoutMs = LEADERBOARD_TIMEOUT_MS) {
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

            // 2. Calculate the 1-based position by counting opted-in profiles ordered ahead, using the
            // same order as getTopBy*: first column, second column, then id for exact ties.
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

                    val exactTiesBefore = postgrest["profiles"].select {
                        filter {
                            eq("leaderboard_opt_in", true)
                            eq("current_streak", targetProfile.currentStreak)
                            eq("longest_streak", targetProfile.longestStreak)
                            lt("id", targetProfile.id)
                        }
                    }.decodeList<CloudProfileDto>().size

                    higherStreaks + sameStreakHigherLongest + exactTiesBefore + 1
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

                    val exactTiesBefore = postgrest["profiles"].select {
                        filter {
                            eq("leaderboard_opt_in", true)
                            eq("level", targetProfile.level)
                            eq("total_xp", targetProfile.totalXp)
                            lt("id", targetProfile.id)
                        }
                    }.decodeList<CloudProfileDto>().size

                    higherLevels + sameLevelHigherXp + exactTiesBefore + 1
                }
            }

            UserLeaderboardRank(rank = rank, profile = targetProfile)
        }
    }

    override suspend fun reportProfile(
        reportedProfileId: String,
        reason: String
    ): SupabaseResult<Unit> {
        val user = auth?.currentUserOrNull()
            ?: return SupabaseResult.AuthError(
                IllegalStateException("Not authenticated"),
                "Please sign in to report an offensive display name."
            )

        return safeSupabaseCall(timeoutMs = LEADERBOARD_TIMEOUT_MS) {
            postgrest["reports"].insert(
                com.pixelquest.app.data.remote.model.ReportDto(
                    reporterId = user.id,
                    reportedProfileId = reportedProfileId,
                    reason = reason
                )
            )
        }
    }

    companion object {
        const val LEADERBOARD_TIMEOUT_MS = 10_000L
    }
}
