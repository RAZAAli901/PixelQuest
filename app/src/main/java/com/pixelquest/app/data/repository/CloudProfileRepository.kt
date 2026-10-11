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

    /**
     * After RESET ALL PROGRESS: this phone's fresh numbers, sent even though the board holds higher
     * ones (the usual push never sends lower numbers; see PendingProgressReset).
     */
    suspend fun sendProgressAfterReset(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
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

        return when (val result = safeSupabaseCall<Unit> { postgrest["profiles"].upsert(dto) }) {
            is SupabaseResult.Success -> result.also { pendingChoice?.confirmed(user.id, optIn) }
            is SupabaseResult.ServerError -> if (result.code == NAME_TAKEN_STATUS) {
                // Another player has this name (names are unique, ignoring case) and nothing changed
                // on the server. The join can't succeed as it stands: this phone is off the board again,
                // keeping the name to edit, and a sync puts back what the server holds.
                pendingChoice?.confirmed(user.id, optIn)
                userProfileRepository.updateLeaderboardSettings(optIn = false, displayName = displayName)
                SupabaseResult.ServerError(NAME_TAKEN_STATUS, NAME_TAKEN)
            } else result
            else -> result
        }
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

        return safeSupabaseCall {
            val remoteProfile = postgrest["profiles"].select {
                filter {
                    eq("id", user.id)
                }
            }.decodeList<CloudProfileDto>().firstOrNull()

            var optIn = profile?.leaderboardOptIn ?: false
            var displayName = profile?.leaderboardDisplayName?.ifBlank { null }
            val pending = pendingChoice?.pendingFor(user.id)
            // Joining or leaving belongs to the account (PendingLeaderboardChoice). With no choice of
            // its own waiting to be sent, this phone follows the account's row: SYNC NOW used to send
            // this phone's flag and undo a leave made on the account's other phone.
            if (pendingChoice != null && pending == null && remoteProfile != null && remoteProfile.leaderboardOptIn != optIn) {
                optIn = remoteProfile.leaderboardOptIn
                displayName = remoteProfile.displayName.ifBlank { null } ?: displayName
                userProfileRepository.updateLeaderboardSettings(optIn = optIn, displayName = displayName)
                if (!optIn) return@safeSupabaseCall false
            }
            // A name, or nobody sees this row: the fallback "Hero" was everyone's, and names are unique.
            displayName = displayName ?: remoteProfile?.displayName?.ifBlank { null }
            if (displayName == null) optIn = false

            val dto = buildProfileDto(
                userId = user.id,
                displayName = displayName.orEmpty(),
                optIn = optIn,
                profile = profile,
                currentStreak = streak?.currentStreak ?: 0,
                longestStreak = streak?.longestStreak ?: 0
            )

            if (remoteProfile != null) {
                val decision = com.pixelquest.app.domain.SyncConflictResolver.evaluate(
                    localCurrentStreak = streak?.currentStreak ?: 0,
                    localLongestStreak = streak?.longestStreak ?: 0,
                    localLevel = profile?.level ?: 1,
                    localTotalXp = profile?.totalXp ?: 0,
                    serverProfile = remoteProfile
                )
                if (decision is com.pixelquest.app.domain.SyncDecision.SkipServerHigherProgress) {
                    return@safeSupabaseCall false
                }
            }

            postgrest["profiles"].upsert(dto)
            if (pending != null) pendingChoice?.confirmed(user.id, optIn)
            true
        }
    }

    override suspend fun sendProgressAfterReset(): SupabaseResult<Unit> {
        val user = auth.currentUserOrNull()
            ?: return SupabaseResult.AuthError(IllegalStateException("No authenticated user"), "Sign in to synchronize profile.")
        val profile = userProfileRepository.getProfile().first()
        val name = profile?.leaderboardDisplayName?.ifBlank { null }
        val streak = streakRepository.getCurrentStreak().first()
        val dto = buildProfileDto(
            userId = user.id,
            displayName = name.orEmpty(),
            optIn = name != null && profile?.leaderboardOptIn == true,
            profile = profile,
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0
        )
        return safeSupabaseCall<Unit> { postgrest["profiles"].upsert(dto) }
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
        /** The status PostgREST answers when a write would take another player's name (23505). */
        const val NAME_TAKEN_STATUS = 409
        const val NAME_TAKEN = "That name is taken on the leaderboard. Try another one."

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
