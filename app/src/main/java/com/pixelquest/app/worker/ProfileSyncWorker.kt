package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker responsible for synchronizing local player progress
 * (streak, level, XP) to the cloud Supabase 'profiles' table.
 *
 * Runs only if the player is authenticated with Supabase AND has explicitly
 * opted into the public leaderboard.
 */
@HiltWorker
class ProfileSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userProfileRepository: UserProfileRepository,
    private val cloudProfileRepository: CloudProfileRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val profile = userProfileRepository.getProfile().first()
            ?: return Result.success()

        // Privacy and authentication guard: only sync if signed in and opted in
        val isCloudLinked = !profile.supabaseUserId.isNullOrBlank()
        val isOptedIn = profile.leaderboardOptIn

        if (!isCloudLinked || !isOptedIn) {
            // Nothing to sync to public leaderboard for offline or non-opted-in users
            return Result.success()
        }

        return when (val syncResult = cloudProfileRepository.syncProfileToCloud()) {
            is SupabaseResult.Success -> Result.success()
            is SupabaseResult.NetworkError -> Result.retry()
            is SupabaseResult.ServerError -> Result.retry()
            is SupabaseResult.AuthError -> Result.failure()
            is SupabaseResult.UnknownError -> Result.retry()
        }
    }
}
