package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto

/**
 * Domain contract for syncing local hero stats and choices to the cloud profile.
 */
interface CloudProfileRepository {
    suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit>
    suspend fun syncProfileToCloud(): SupabaseResult<Unit>

    /**
     * [syncProfileToCloud] that says whether anything was sent: Success(false) when the leaderboard
     * already holds more progress for this account and the push was skipped.
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
     * ones (the usual push never sends lower numbers).
     */
    suspend fun sendProgressAfterReset(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    suspend fun deleteCloudProfile(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
}
