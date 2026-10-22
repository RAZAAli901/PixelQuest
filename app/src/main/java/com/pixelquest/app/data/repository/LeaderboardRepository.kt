package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import javax.inject.Inject
import javax.inject.Singleton
import com.pixelquest.app.domain.repository.LeaderboardRepository as DomainLeaderboardRepository
import com.pixelquest.app.domain.repository.LeaderboardSortMode as DomainLeaderboardSortMode
import com.pixelquest.app.domain.repository.LeaderboardWindow as DomainLeaderboardWindow
import com.pixelquest.app.domain.repository.PlayersAroundYou as DomainPlayersAroundYou
import com.pixelquest.app.domain.repository.RankedProfile as DomainRankedProfile
import com.pixelquest.app.domain.repository.UserLeaderboardRank as DomainUserLeaderboardRank

typealias LeaderboardSortMode = DomainLeaderboardSortMode
typealias UserLeaderboardRank = DomainUserLeaderboardRank
typealias RankedProfile = DomainRankedProfile
typealias PlayersAroundYou = DomainPlayersAroundYou
val LeaderboardWindow = DomainLeaderboardWindow
typealias LeaderboardRepository = DomainLeaderboardRepository

@Singleton
open class LeaderboardRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth? = null,
) : LeaderboardRepository {

    /**
     * The board is for signed-in players: signed out, the server refuses the table (S010), so the
     * board is simply empty, as it always looked, without asking.
     */
    private fun signedOut(): Boolean = auth?.sessionStatus?.value is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated

    override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> {
        if (signedOut()) return SupabaseResult.Success(emptyList())
        return safeSupabaseCall(timeoutMs = LEADERBOARD_TIMEOUT_MS) {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        if (signedOut()) return SupabaseResult.Success(emptyList())
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
        if (signedOut()) return SupabaseResult.Success(emptyList())
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
        userId: String?,
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
                    val higherStreaks = countProfiles {
                        eq("leaderboard_opt_in", true)
                        gt("current_streak", targetProfile.currentStreak)
                    }

                    val sameStreakHigherLongest = countProfiles {
                        eq("leaderboard_opt_in", true)
                        eq("current_streak", targetProfile.currentStreak)
                        gt("longest_streak", targetProfile.longestStreak)
                    }

                    val exactTiesBefore = countProfiles {
                        eq("leaderboard_opt_in", true)
                        eq("current_streak", targetProfile.currentStreak)
                        eq("longest_streak", targetProfile.longestStreak)
                        lt("id", targetProfile.id)
                    }

                    higherStreaks + sameStreakHigherLongest + exactTiesBefore + 1
                }
                LeaderboardSortMode.LEVEL -> {
                    val higherLevels = countProfiles {
                        eq("leaderboard_opt_in", true)
                        gt("level", targetProfile.level)
                    }

                    val sameLevelHigherXp = countProfiles {
                        eq("leaderboard_opt_in", true)
                        eq("level", targetProfile.level)
                        gt("total_xp", targetProfile.totalXp)
                    }

                    val exactTiesBefore = countProfiles {
                        eq("leaderboard_opt_in", true)
                        eq("level", targetProfile.level)
                        eq("total_xp", targetProfile.totalXp)
                        lt("id", targetProfile.id)
                    }

                    higherLevels + sameLevelHigherXp + exactTiesBefore + 1
                }
            }

            UserLeaderboardRank(rank = rank, profile = targetProfile)
        }
    }

    override suspend fun reportProfile(
        reportedProfileId: String,
        reason: String,
    ): SupabaseResult<Unit> {
        val user = auth?.currentUserOrNull()
            ?: return SupabaseResult.AuthError(
                IllegalStateException("Not authenticated"),
                "Please sign in to report an offensive display name.",
            )

        return safeSupabaseCall(timeoutMs = LEADERBOARD_TIMEOUT_MS) {
            postgrest["reports"].insert(
                com.pixelquest.app.data.remote.model.ReportDto(
                    reporterId = user.id,
                    reportedProfileId = reportedProfileId,
                    reason = reason,
                ),
            )
        }
    }

    /**
     * Counts matching profiles on the server (a HEAD request; the total comes back in Content-Range).
     * It used to fetch their ids and count them, but the API returns at most 1000 rows per request,
     * so anyone with more than 1000 heroes ahead was shown a rank of about 1000.
     */
    private suspend fun countProfiles(filters: PostgrestFilterBuilder.() -> Unit): Int =
        postgrest["profiles"].select(columns = Columns.list("id")) {
            head = true
            count(Count.EXACT)
            filter(filters)
        }.countOrNull()?.toInt() ?: throw IllegalStateException("The leaderboard count was missing from the response")

    companion object {
        const val LEADERBOARD_TIMEOUT_MS = 10_000L
    }
}
