package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto

enum class LeaderboardSortMode {
    STREAK,
    LEVEL,
}

/** A profile with its 1-based position on the leaderboard. */
data class RankedProfile(
    val rank: Int,
    val profile: CloudProfileDto,
)

/** The signed-in user's rank on the leaderboard (alias for [RankedProfile] to eliminate duplicate class). */
typealias UserLeaderboardRank = RankedProfile

/** The signed-in hero's rank plus the heroes just above and below them. */
data class PlayersAroundYou(
    val you: UserLeaderboardRank,
    val entries: List<RankedProfile>,
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

/**
 * Domain contract for querying leaderboard positions, ranks, and reporting accounts.
 */
interface LeaderboardRepository {
    suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByStreak(limit: Long = 20, offset: Long = 0): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByLevel(limit: Long = 20, offset: Long = 0): SupabaseResult<List<CloudProfileDto>>
    suspend fun getCurrentUserRank(
        sortMode: LeaderboardSortMode,
        userId: String? = null,
    ): SupabaseResult<UserLeaderboardRank?>
    suspend fun reportProfile(
        reportedProfileId: String,
        reason: String,
    ): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    /**
     * The heroes ranked just above and below the signed-in user: their rank, then one page of the
     * same ordered list around it. Null when the user isn't on the leaderboard.
     */
    suspend fun getPlayersAroundYou(
        sortMode: LeaderboardSortMode,
        userId: String? = null,
        radius: Int = 3,
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
