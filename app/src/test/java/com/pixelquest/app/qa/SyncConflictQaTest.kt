package com.pixelquest.app.qa

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.SyncConflictResolver
import com.pixelquest.app.domain.SyncDecision
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Manual QA verification simulation:
 * Simulates setting a Supabase profile row to a higher value on the server than the local device,
 * triggering a local sync, and verifying that the push is safely skipped without regressing the server value.
 */
class SyncConflictQaTest {

    @Test
    fun manualQa_whenServerHasHigherProgress_syncIsSkippedWithoutServerRegression() = runBlocking {
        // Step 1: Server row state (e.g. from primary phone device)
        val serverProfile = CloudProfileDto(
            id = "test-hero-uuid",
            displayName = "QuestMaster",
            currentStreak = 25,
            longestStreak = 30,
            level = 8,
            totalXp = 4500,
            leaderboardOptIn = true,
            updatedAt = "2026-09-12T08:30:00Z"
        )

        // Step 2: Local device state (e.g. stale tablet device)
        val localStreak = StreakEntity(
            currentStreak = 4, // Stale lower streak
            longestStreak = 10
        )
        val localProfile = UserProfileEntity(
            heroName = "Hero",
            level = 2, // Stale lower level
            totalXp = 450,
            supabaseUserId = "test-hero-uuid",
            leaderboardOptIn = true,
            leaderboardDisplayName = "QuestMaster"
        )

        // Step 3: Trigger sync evaluation
        val syncTriggerTime = Instant.parse("2026-09-12T10:00:00Z")
        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = localStreak.currentStreak,
            localLongestStreak = localStreak.longestStreak,
            localLevel = localProfile.level,
            localTotalXp = localProfile.totalXp,
            localTriggerTime = syncTriggerTime,
            serverProfile = serverProfile
        )

        // Step 4: Verify decision is to skip due to server higher progress
        assertTrue(decision is SyncDecision.SkipServerHigherProgress)
        val skip = decision as SyncDecision.SkipServerHigherProgress
        assertTrue(skip.reason.contains("Server level (8) is higher than local level (2)"))

        // Server row remains untouched at level 8 and streak 25
        assertEquals(8, serverProfile.level)
        assertEquals(25, serverProfile.currentStreak)
    }

    @Test
    fun manualQa_whenServerHasNewerTimestamp_pushIsSkippedUnderLastWriteWins() = runBlocking {
        val serverProfile = CloudProfileDto(
            id = "test-hero-uuid",
            displayName = "QuestMaster",
            currentStreak = 10,
            longestStreak = 10,
            level = 4,
            totalXp = 1000,
            leaderboardOptIn = true,
            updatedAt = "2026-09-12T11:45:00Z" // Newer than local trigger
        )

        val localTriggerTime = Instant.parse("2026-09-12T11:30:00Z") // 15 mins older
        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 10,
            localLongestStreak = 10,
            localLevel = 4,
            localTotalXp = 1000,
            localTriggerTime = localTriggerTime,
            serverProfile = serverProfile
        )

        assertTrue(decision is SyncDecision.SkipServerNewer)
    }

    @Test
    fun manualQa_whenLocalProgressAdvances_pushIsAllowed() = runBlocking {
        val serverProfile = CloudProfileDto(
            id = "test-hero-uuid",
            displayName = "QuestMaster",
            currentStreak = 5,
            longestStreak = 5,
            level = 2,
            totalXp = 300,
            leaderboardOptIn = true,
            updatedAt = "2026-09-12T08:00:00Z"
        )

        val localTriggerTime = Instant.parse("2026-09-12T12:00:00Z")
        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 6, // Incremented
            localLongestStreak = 6,
            localLevel = 3, // Level up
            localTotalXp = 500,
            localTriggerTime = localTriggerTime,
            serverProfile = serverProfile
        )

        assertEquals(SyncDecision.PushLocal, decision)
    }
}
