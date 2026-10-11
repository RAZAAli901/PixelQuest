package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface CloudProfileRepository {
    suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit>
    suspend fun syncProfileToCloud(): SupabaseResult<Unit>

    /**
     * [syncProfileToCloud] that says whether anything was sent: Success(false) when the leaderboard
     * already holds more progress for this account (from another device) and the push was skipped.
     */
    suspend fun pushProfileToCloud(): SupabaseResult<Boolean> = when (val result = syncProfileToCloud()) {
        is SupabaseResult.Success -> SupabaseResult.Success(true)
        is SupabaseResult.NetworkError -> result
        is SupabaseResult.AuthError -> result
        is SupabaseResult.ServerError -> result
        is SupabaseResult.UnknownError -> result
    }

    suspend fun fetchCloudProfile(userId: String): SupabaseResult<CloudProfileDto?> = SupabaseResult.Success(null)
    suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    suspend fun deleteCloudProfile(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
}

@Singleton
open class CloudProfileRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val userProfileRepository: UserProfileRepository,
    private val streakRepository: StreakRepository,
    private val pendingChoice: PendingLeaderboardChoice? = null
) : CloudProfileRepository {

    /** The account the choice belongs to: signed in, or still linked while its session can't refresh. */
    private suspend fun accountId(): String? =
        auth.currentUserOrNull()?.id ?: userProfileRepository.getProfile().first()?.supabaseUserId

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        // 1. Update local Room database, and keep the choice until the server has it
        userProfileRepository.updateLeaderboardSettings(optIn = optIn, displayName = displayName)
        accountId()?.let { pendingChoice?.choose(it, optIn, displayName) }

        // 2. Sync to Supabase
        val user = auth.currentUserOrNull()
            ?: return SupabaseResult.AuthError(
                IllegalStateException("No authenticated Supabase user"),
                "Please sign in to update cloud leaderboard."
            )

        val profile = userProfileRepository.getProfile().first()
        val streak = streakRepository.getCurrentStreak().first()

        val dto = buildProfileDto(
            userId = user.id,
            displayName = displayName,
            optIn = optIn,
            profile = profile,
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0
        )

        return safeSupabaseCall<Unit> {
            postgrest["profiles"].upsert(dto)
        }.also { if (it is SupabaseResult.Success) pendingChoice?.confirmed(user.id, optIn) }
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> = when (val result = pushProfileToCloud()) {
        is SupabaseResult.Success -> SupabaseResult.Success(Unit)
        is SupabaseResult.NetworkError -> result
        is SupabaseResult.AuthError -> result
        is SupabaseResult.ServerError -> result
        is SupabaseResult.UnknownError -> result
    }

    override suspend fun pushProfileToCloud(): SupabaseResult<Boolean> {
        val user = auth.currentUserOrNull()
            ?: return SupabaseResult.AuthError(
                IllegalStateException("No authenticated user"),
                "Sign in to synchronize profile."
            )

        val profile = userProfileRepository.getProfile().first()
        val streak = streakRepository.getCurrentStreak().first()

        val displayName = profile?.leaderboardDisplayName?.ifBlank { "Hero" } ?: "Hero"

        val dto = buildProfileDto(
            userId = user.id,
            displayName = displayName,
            optIn = profile?.leaderboardOptIn ?: false,
            profile = profile,
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0
        )

        return safeSupabaseCall {
            val remoteProfile = postgrest["profiles"].select {
                filter {
                    eq("id", user.id)
                }
            }.decodeList<CloudProfileDto>().firstOrNull()

            if (remoteProfile != null) {
                val decision = com.pixelquest.app.domain.SyncConflictResolver.evaluate(
                    localCurrentStreak = streak?.currentStreak ?: 0,
                    localLongestStreak = streak?.longestStreak ?: 0,
                    localLevel = profile?.level ?: 1,
                    localTotalXp = profile?.totalXp ?: 0,
                    localTriggerTime = Instant.now(),
                    serverProfile = remoteProfile
                )
                if (decision is com.pixelquest.app.domain.SyncDecision.SkipServerHigherProgress) {
                    return@safeSupabaseCall false
                }
            }

            postgrest["profiles"].upsert(dto)
            true
        }
    }

    override suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> {
        userProfileRepository.updateLeaderboardOptIn(false)
        accountId()?.let { pendingChoice?.choose(it, optIn = false, displayName = null) }

        // Not signed in right now: the choice stays pending and the sync worker sends it later.
        val user = auth.currentUserOrNull()
            ?: return SupabaseResult.Success(Unit)

        // Only the flag, and only on a row that exists. This used to upsert a full row (level, XP,
        // streaks, display name): a spectator who never joined uploaded their stats.
        return safeSupabaseCall<Unit> {
            postgrest["profiles"].update({ set("leaderboard_opt_in", false) }) {
                filter { eq("id", user.id) }
            }
        }.also { if (it is SupabaseResult.Success) pendingChoice?.confirmed(user.id, optIn = false) }
    }

    override suspend fun fetchCloudProfile(userId: String): SupabaseResult<CloudProfileDto?> {
        return safeSupabaseCall {
            postgrest["profiles"].select {
                filter {
                    eq("id", userId)
                }
            }.decodeList<CloudProfileDto>().firstOrNull()
        }
    }

    override suspend fun deleteCloudProfile(): SupabaseResult<Unit> {
        val user = auth.currentUserOrNull()
            ?: return SupabaseResult.AuthError(
                IllegalStateException("No authenticated Supabase user"),
                "Please sign in to delete cloud data."
            )

        return safeSupabaseCall {
            postgrest["profiles"].delete {
                filter {
                    eq("id", user.id)
                }
            }
        }
    }

    companion object {
        fun buildProfileDto(
            userId: String,
            displayName: String,
            optIn: Boolean,
            profile: UserProfileEntity?,
            currentStreak: Int,
            longestStreak: Int,
            timestamp: String = Instant.now().toString()
        ): CloudProfileDto {
            return CloudProfileDto(
                id = userId,
                displayName = displayName,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                level = profile?.level ?: 1,
                totalXp = profile?.totalXp ?: 0,
                leaderboardOptIn = optIn,
                updatedAt = timestamp
            )
        }
    }
}
