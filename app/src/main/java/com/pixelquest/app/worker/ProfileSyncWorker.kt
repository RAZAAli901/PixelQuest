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
    private val streakRepository: com.pixelquest.app.domain.repository.StreakRepository,
    private val pendingChoice: com.pixelquest.app.data.repository.PendingLeaderboardChoice? = null,
    private val pendingProgressReset: com.pixelquest.app.data.repository.PendingProgressReset? = null
) : CoroutineWorker(appContext, workerParams) {

    private fun outcome(result: SupabaseResult<*>): Result = when (result) {
        is SupabaseResult.Success -> Result.success()
        is SupabaseResult.NetworkError -> Result.retry()
        // The server refused this request (a name its filter rejects, say): sending it again gets the
        // same answer, and used to retry with backoff for as long as the app was installed. The next
        // completion or SYNC NOW starts a fresh sync. Outages, rate limits and an expired session
        // (401, refreshed meanwhile) are worth retrying.
        is SupabaseResult.ServerError -> if (result.code in REFUSED) Result.failure() else Result.retry()
        is SupabaseResult.AuthError -> Result.failure()
        is SupabaseResult.UnknownError -> Result.retry()
    }

    override suspend fun doWork(): Result {
        return try {
            val profile = userProfileRepository.getProfile().first()
                ?: return Result.success()

            // Privacy and authentication guard: user must have a linked cloud account
            val isCloudLinked = !profile.supabaseUserId.isNullOrBlank()
            if (!isCloudLinked) {
                // Nothing to sync to public leaderboard for offline users
                return Result.success()
            }

            val accountId = profile.supabaseUserId!!

            // Joining or leaving belongs to the account (see PendingLeaderboardChoice). A choice made
            // on this phone that the server hasn't confirmed is the newest: send it.
            pendingChoice?.startTrackingAfterUpdate(accountId, profile.leaderboardOptIn)
            val pending = pendingChoice?.pendingFor(accountId)
            if (pending != null && !pending.optIn) {
                val left = cloudProfileRepository.optOutFromLeaderboard()
                // Not signed in at the moment (a session that can't refresh): the call sends nothing
                // and says so by leaving the choice pending. Try again rather than call it done.
                if (left is SupabaseResult.Success && pendingChoice?.pendingFor(accountId) != null) return Result.retry()
                return outcome(left)
            }
            if (pending != null) {
                val name = pending.displayName ?: profile.leaderboardDisplayName.orEmpty()
                return outcome(cloudProfileRepository.updateOptInAndSync(optIn = true, displayName = name))
            }

            // The server's row, when it was already read for the choice below.
            var serverRead: SupabaseResult<com.pixelquest.app.data.remote.model.CloudProfileDto?>? = null
            if (pendingChoice == null) {
                // Without the pending store (older callers): the phone's own flag decides, as before.
                if (!profile.leaderboardOptIn) return outcome(cloudProfileRepository.optOutFromLeaderboard())
            } else {
                // Otherwise the server's row holds the account's choice, made on this phone or another.
                val fetched = cloudProfileRepository.fetchCloudProfile(accountId)
                val server = when (fetched) {
                    is SupabaseResult.Success -> fetched.data
                    else -> return outcome(fetched)
                }
                serverRead = fetched
                val serverName = server?.displayName?.ifBlank { null }
                when {
                    // Never joined: a spectator uploads nothing.
                    server == null && !profile.leaderboardOptIn -> return Result.success()
                    server != null && (server.leaderboardOptIn != profile.leaderboardOptIn ||
                        (server.leaderboardOptIn && serverName != null && serverName != profile.leaderboardDisplayName)) -> {
                        userProfileRepository.updateLeaderboardSettings(
                            optIn = server.leaderboardOptIn,
                            displayName = serverName ?: profile.leaderboardDisplayName
                        )
                        if (!server.leaderboardOptIn) return Result.success()
                    }
                    !profile.leaderboardOptIn -> return Result.success()
                }
            }

            // RESET ALL PROGRESS on this phone: its fresh numbers go to the board once, though the
            // board holds higher ones (the checks below would hold them back for good).
            if (pendingProgressReset?.isPendingFor(accountId) == true) {
                val sent = cloudProfileRepository.sendProgressAfterReset()
                if (sent is SupabaseResult.Success) pendingProgressReset.sent(accountId)
                return outcome(sent)
            }

            // The anti-regression guard (SyncConflictResolver): never lower the board's numbers.

            val serverProfileResult = serverRead ?: cloudProfileRepository.fetchCloudProfile(accountId)
            val serverProfile = when (serverProfileResult) {
                is SupabaseResult.Success -> serverProfileResult.data
                else -> return outcome(serverProfileResult)
            }

            val streak = streakRepository.getCurrentStreak().first()
            val decision = com.pixelquest.app.domain.SyncConflictResolver.evaluate(
                localCurrentStreak = streak?.currentStreak ?: 0,
                localLongestStreak = streak?.longestStreak ?: 0,
                localLevel = profile.level,
                localTotalXp = profile.totalXp,
                serverProfile = serverProfile
            )

            when (decision) {
                is com.pixelquest.app.domain.SyncDecision.SkipServerHigherProgress -> {
                    // Defensive check: server holds higher streak/level/xp than local.
                    // Never push lower values to avoid regressing user progress.
                    return Result.success()
                }
                is com.pixelquest.app.domain.SyncDecision.PushLocal -> {
                    // Local values are higher or equal; proceed with push
                }
            }

            outcome(cloudProfileRepository.syncProfileToCloud())
        } catch (e: Exception) {
            // Graceful degradation: Supabase outage or transient network crash retries via WorkManager backoff
            // without blocking local app functions (alarms, quests, leveling, etc.)
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            com.pixelquest.app.notification.NotificationChannels.Spec.SYNC.id
        )
            .setSmallIcon(com.pixelquest.app.notification.PqNotifications.SMALL_ICON)
            .setContentTitle("PixelQuest Sync")
            .setContentText("Syncing hero stats to leaderboard...")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
        return androidx.work.ForegroundInfo(SYNC_NOTIFICATION_ID, notification)
    }

    companion object {
        /** Statuses for a request the server refused as it stands: bad request, forbidden, conflict, unprocessable. */
        private val REFUSED = setOf(400, 403, 404, 409, 422)

        const val SYNC_NOTIFICATION_ID = 9001
    }
}
