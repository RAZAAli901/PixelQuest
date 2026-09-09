package com.pixelquest.app.data.repository

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
    suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
}

@Singleton
open class CloudProfileRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val userProfileRepository: UserProfileRepository,
    private val streakRepository: StreakRepository
) : CloudProfileRepository {

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        // 1. Update local Room database
        userProfileRepository.updateLeaderboardSettings(optIn = optIn, displayName = displayName)

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

        return safeSupabaseCall {
            postgrest["profiles"].upsert(dto)
        }
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
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
            postgrest["profiles"].upsert(dto)
        }
    }

    override suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> {
        userProfileRepository.updateLeaderboardOptIn(false)

        val user = auth.currentUserOrNull()
            ?: return SupabaseResult.Success(Unit)

        val profile = userProfileRepository.getProfile().first()
        val streak = streakRepository.getCurrentStreak().first()
        val displayName = profile?.leaderboardDisplayName?.ifBlank { "Hero" } ?: "Hero"

        val dto = buildProfileDto(
            userId = user.id,
            displayName = displayName,
            optIn = false,
            profile = profile,
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0
        )

        return safeSupabaseCall {
            postgrest["profiles"].upsert(dto)
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
