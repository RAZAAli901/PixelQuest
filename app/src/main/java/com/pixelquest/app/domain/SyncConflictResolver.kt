package com.pixelquest.app.domain

import com.pixelquest.app.data.remote.model.CloudProfileDto
import java.time.Instant

/**
 * Defines conflict resolution and staleness protection rules for PixelQuest cloud synchronization.
 *
 * Applicable when the same account is signed in on two devices concurrently.
 *
 * Conflict Resolution Semantics:
 * 1. Last-Write-Wins (LWW) by updated_at timestamp:
 *    If the server's updated_at timestamp is strictly newer than the local event trigger timestamp,
 *    the push is skipped to avoid overwriting a newer state produced by another device.
 *
 * 2. Defensive Anti-Regression Guard (Monotonic Progress):
 *    A sync must NEVER overwrite higher streak, level, or total XP values on the server with
 *    lower local values, preventing a stale or neglected secondary device from wiping out
 *    real progress achieved on a primary device.
 */
sealed class SyncDecision {
    object PushLocal : SyncDecision()
    data class SkipServerNewer(val serverUpdatedAt: String, val localTriggerTime: String) : SyncDecision()
    data class SkipServerHigherProgress(val reason: String) : SyncDecision()
}

object SyncConflictResolver {

    /**
     * Evaluates whether a local profile state should be pushed to Supabase.
     *
     * @param localCurrentStreak current streak on this device
     * @param localLongestStreak longest streak on this device
     * @param localLevel character level on this device
     * @param localTotalXp accumulated XP on this device
     * @param localTriggerTime timestamp when the local event/sync was initiated
     * @param serverProfile current row fetched from Supabase profiles table, or null if no remote row exists
     * @return SyncDecision indicating whether to push or skip with rationale
     */
    fun evaluate(
        localCurrentStreak: Int,
        localLongestStreak: Int,
        localLevel: Int,
        localTotalXp: Int,
        localTriggerTime: Instant,
        serverProfile: CloudProfileDto?
    ): SyncDecision {
        if (serverProfile == null) {
            return SyncDecision.PushLocal
        }

        // Rule 1: Monotonic Progress Guard (Staleness Protection)
        // If the server currently holds higher level, streak, or XP, never regress cloud progress.
        if (serverProfile.level > localLevel) {
            return SyncDecision.SkipServerHigherProgress(
                "Server level (${serverProfile.level}) is higher than local level ($localLevel)"
            )
        }
        if (serverProfile.currentStreak > localCurrentStreak) {
            return SyncDecision.SkipServerHigherProgress(
                "Server current streak (${serverProfile.currentStreak}) is higher than local streak ($localCurrentStreak)"
            )
        }
        if (serverProfile.longestStreak > localLongestStreak) {
            return SyncDecision.SkipServerHigherProgress(
                "Server longest streak (${serverProfile.longestStreak}) is higher than local longest streak ($localLongestStreak)"
            )
        }
        if (serverProfile.totalXp > localTotalXp) {
            return SyncDecision.SkipServerHigherProgress(
                "Server total XP (${serverProfile.totalXp}) is higher than local total XP ($localTotalXp)"
            )
        }

        // Rule 2: Last-Write-Wins (LWW) by updated_at timestamp comparison
        val serverUpdatedAt = serverProfile.updatedAt
        if (!serverUpdatedAt.isNullOrBlank()) {
            try {
                val serverTime = Instant.parse(serverUpdatedAt)
                if (serverTime.isAfter(localTriggerTime)) {
                    return SyncDecision.SkipServerNewer(
                        serverUpdatedAt = serverUpdatedAt,
                        localTriggerTime = localTriggerTime.toString()
                    )
                }
            } catch (_: Exception) {
                // If timestamp parsing fails, proceed with push assuming local is valid
            }
        }

        return SyncDecision.PushLocal
    }
}
