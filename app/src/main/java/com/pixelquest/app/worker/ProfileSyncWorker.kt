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
    private val cloudProfileRepository: CloudProfileRepository,
    private val streakRepository: com.pixelquest.app.domain.repository.StreakRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val profile = userProfileRepository.getProfile().first()
            ?: return Result.success()

        // Privacy and authentication guard: user must have a linked cloud account
        val isCloudLinked = !profile.supabaseUserId.isNullOrBlank()
        if (!isCloudLinked) {
            // Nothing to sync to public leaderboard for offline users
            return Result.success()
        }

        val isOptedIn = profile.leaderboardOptIn
        if (!isOptedIn) {
            // Safeguard: Ensure server-side leaderboard_opt_in is false promptly,
            // never leaving a dangling opted-in record on the cloud
            return when (cloudProfileRepository.optOutFromLeaderboard()) {
                is SupabaseResult.Success -> Result.success()
                is SupabaseResult.NetworkError -> Result.retry()
                is SupabaseResult.ServerError -> Result.retry()
                is SupabaseResult.AuthError -> Result.failure()
                is SupabaseResult.UnknownError -> Result.retry()
            }
        }

        // Check server state for multi-device Last-Write-Wins and Anti-Regression guards
        val triggerEpochMs = inputData.getLong(KEY_TRIGGER_TIMESTAMP, System.currentTimeMillis())
        val localTriggerTime = java.time.Instant.ofEpochMilli(triggerEpochMs)

        val serverProfileResult = cloudProfileRepository.fetchCloudProfile(profile.supabaseUserId!!)
        val serverProfile = when (serverProfileResult) {
            is SupabaseResult.Success -> serverProfileResult.data
            is SupabaseResult.NetworkError -> return Result.retry()
            is SupabaseResult.ServerError -> return Result.retry()
            is SupabaseResult.AuthError -> return Result.failure()
            is SupabaseResult.UnknownError -> return Result.retry()
        }

        val streak = streakRepository.getCurrentStreak().first()
        val decision = com.pixelquest.app.domain.SyncConflictResolver.evaluate(
            localCurrentStreak = streak?.currentStreak ?: 0,
            localLongestStreak = streak?.longestStreak ?: 0,
            localLevel = profile.level,
            localTotalXp = profile.totalXp,
            localTriggerTime = localTriggerTime,
            serverProfile = serverProfile
        )

        when (decision) {
            is com.pixelquest.app.domain.SyncDecision.SkipServerHigherProgress -> {
                // Defensive check: server holds higher streak/level/xp than local.
                // Never push lower values to avoid regressing user progress.
                return Result.success()
            }
            is com.pixelquest.app.domain.SyncDecision.SkipServerNewer -> {
                // Last-Write-Wins: server was updated more recently by another device.
                return Result.success()
            }
            is com.pixelquest.app.domain.SyncDecision.PushLocal -> {
                // Local values are higher or equal; proceed with push
            }
        }

        return when (val syncResult = cloudProfileRepository.syncProfileToCloud()) {
            is SupabaseResult.Success -> Result.success()
            is SupabaseResult.NetworkError -> Result.retry()
            is SupabaseResult.ServerError -> Result.retry()
            is SupabaseResult.AuthError -> Result.failure()
            is SupabaseResult.UnknownError -> Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            com.pixelquest.app.notification.NotificationHelper.CHANNEL_ID
        )
            .setSmallIcon(com.pixelquest.app.R.drawable.ic_tasks)
            .setContentTitle("PixelQuest Sync")
            .setContentText("Syncing hero stats to leaderboard...")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
        return androidx.work.ForegroundInfo(SYNC_NOTIFICATION_ID, notification)
    }

    companion object {
        const val SYNC_NOTIFICATION_ID = 9001
        const val KEY_TRIGGER_TIMESTAMP = "key_trigger_timestamp"
    }
}
