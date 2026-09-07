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

        val dto = CloudProfileDto(
            id = user.id,
            displayName = displayName,
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0,
            level = profile?.level ?: 1,
            totalXp = profile?.totalXp ?: 0,
            leaderboardOptIn = optIn,
            updatedAt = Instant.now().toString()
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

        val dto = CloudProfileDto(
            id = user.id,
            displayName = displayName,
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0,
            level = profile?.level ?: 1,
            totalXp = profile?.totalXp ?: 0,
            leaderboardOptIn = profile?.leaderboardOptIn ?: false,
            updatedAt = Instant.now().toString()
        )

        return safeSupabaseCall {
            postgrest["profiles"].upsert(dto)
        }
    }
}
