package com.pixelquest.app.domain.policy

/**
 * Step 27: Explicit data-layer policy confirming that Simple Mode and Leaderboard opt-in
 * coexistence is supported, preventing accidental mutual exclusion or disablement.
 */
object LeaderboardSimpleModePolicy {
    /**
     * Determines whether leaderboard opt-in is permitted while Simple Mode is active.
     * Simple Mode deliberately allows leaderboard participation without restriction.
     */
    fun isLeaderboardAllowed(isSimpleModeEnabled: Boolean): Boolean {
        // Coexistence is explicitly supported: Simple Mode users can opt into leaderboard
        return true
    }

    /**
     * Validates whether the given combination of Simple Mode and Leaderboard opt-in is supported.
     */
    fun isCoexistenceSupported(isSimpleModeEnabled: Boolean, isLeaderboardOptedIn: Boolean): Boolean {
        // All states are valid and non-conflicting
        return true
    }
}
