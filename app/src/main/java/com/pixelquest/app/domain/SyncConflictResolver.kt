package com.pixelquest.app.domain

import com.pixelquest.app.data.remote.model.CloudProfileDto

/**
 * Defines conflict resolution and staleness protection rules for PixelQuest cloud synchronization.
 *
 * Applicable when the same account is signed in on two devices concurrently.
 *
 * Anti-regression guard (monotonic progress): a sync never overwrites a higher longest streak,
 * level or total XP on the server with lower local values, so a stale or neglected second device
 * can't wipe out progress made on the main one. (RESET ALL PROGRESS goes past it once, on purpose:
 * PendingProgressReset.)
 *
 * There used to be a second rule, last-write-wins: skip the push when the server's updated_at was
 * later than this phone's clock. The server stamps updated_at with its own clock, so on a phone
 * whose clock ran a little behind, a push soon after the previous one was skipped, and nothing
 * retried it. The guard above already keeps a stale phone from lowering anything.
 */
sealed class SyncDecision {
    object PushLocal : SyncDecision()
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
     * @param serverProfile current row fetched from Supabase profiles table, or null if no remote row exists
     * @return SyncDecision indicating whether to push or skip with rationale
     */
    fun evaluate(
        localCurrentStreak: Int,
        localLongestStreak: Int,
        localLevel: Int,
        localTotalXp: Int,
        serverProfile: CloudProfileDto?
    ): SyncDecision {
        if (serverProfile == null) {
            return SyncDecision.PushLocal
        }

        // Rule 1: Monotonic Progress Guard (Staleness Protection)
        // If the server holds a higher level, longest streak or XP, never regress cloud progress.
        // The current streak is not guarded: it legitimately drops to 0 when a streak breaks, and
        // guarding it froze every later sync (XP, level and the broken streak) on the leaderboard.
        if (serverProfile.level > localLevel) {
            return SyncDecision.SkipServerHigherProgress(
                "Server level (${serverProfile.level}) is higher than local level ($localLevel)"
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

        return SyncDecision.PushLocal
    }
}
