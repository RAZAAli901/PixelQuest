package com.pixelquest.app.domain

import com.pixelquest.app.data.remote.model.CloudProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SyncConflictResolverTest {

    private val baseTriggerTime = Instant.parse("2026-09-12T12:00:00Z")

    @Test
    fun evaluate_whenServerProfileIsNull_returnsPushLocal() {
        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 5,
            localLongestStreak = 10,
            localLevel = 3,
            localTotalXp = 500,
            localTriggerTime = baseTriggerTime,
            serverProfile = null
        )

        assertEquals(SyncDecision.PushLocal, decision)
    }

    @Test
    fun evaluate_whenServerHasHigherLevel_returnsSkipServerHigherProgress() {
        val serverProfile = CloudProfileDto(
            id = "user-1",
            displayName = "Hero",
            currentStreak = 2,
            longestStreak = 2,
            level = 5, // Higher than local
            totalXp = 100,
            leaderboardOptIn = true,
            updatedAt = "2026-09-12T11:00:00Z"
        )

        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 2,
            localLongestStreak = 2,
            localLevel = 3, // Lower than server
            localTotalXp = 100,
            localTriggerTime = baseTriggerTime,
            serverProfile = serverProfile
        )

        assertTrue(decision is SyncDecision.SkipServerHigherProgress)
        assertTrue((decision as SyncDecision.SkipServerHigherProgress).reason.contains("Server level (5) is higher"))
    }

    @Test
    fun evaluate_whenServerHasHigherCurrentStreak_returnsSkipServerHigherProgress() {
        val serverProfile = CloudProfileDto(
            id = "user-1",
            displayName = "Hero",
            currentStreak = 10, // Higher than local
            longestStreak = 10,
            level = 3,
            totalXp = 500,
            leaderboardOptIn = true,
            updatedAt = "2026-09-12T11:00:00Z"
        )

        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 4, // Stale lower streak
            localLongestStreak = 10,
            localLevel = 3,
            localTotalXp = 500,
            localTriggerTime = baseTriggerTime,
            serverProfile = serverProfile
        )

        assertTrue(decision is SyncDecision.SkipServerHigherProgress)
        assertTrue((decision as SyncDecision.SkipServerHigherProgress).reason.contains("Server current streak (10) is higher"))
    }

    @Test
    fun evaluate_whenServerHasHigherLongestStreak_returnsSkipServerHigherProgress() {
        val serverProfile = CloudProfileDto(
            id = "user-1",
            displayName = "Hero",
            currentStreak = 4,
            longestStreak = 15, // Higher than local
            level = 3,
            totalXp = 500,
            leaderboardOptIn = true,
            updatedAt = "2026-09-12T11:00:00Z"
        )

        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 4,
            localLongestStreak = 10, // Stale lower
            localLevel = 3,
            localTotalXp = 500,
            localTriggerTime = baseTriggerTime,
            serverProfile = serverProfile
        )

        assertTrue(decision is SyncDecision.SkipServerHigherProgress)
        assertTrue((decision as SyncDecision.SkipServerHigherProgress).reason.contains("Server longest streak (15) is higher"))
    }

    @Test
    fun evaluate_whenServerHasHigherTotalXp_returnsSkipServerHigherProgress() {
        val serverProfile = CloudProfileDto(
            id = "user-1",
            displayName = "Hero",
            currentStreak = 4,
            longestStreak = 4,
            level = 3,
            totalXp = 2500, // Higher than local
            leaderboardOptIn = true,
            updatedAt = "2026-09-12T11:00:00Z"
        )

        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 4,
            localLongestStreak = 4,
            localLevel = 3,
            localTotalXp = 1200, // Stale lower XP
            localTriggerTime = baseTriggerTime,
            serverProfile = serverProfile
        )

        assertTrue(decision is SyncDecision.SkipServerHigherProgress)
        assertTrue((decision as SyncDecision.SkipServerHigherProgress).reason.contains("Server total XP (2500) is higher"))
    }

    @Test
    fun evaluate_whenServerTimestampIsNewer_returnsSkipServerNewer() {
        val newerServerTime = "2026-09-12T13:00:00Z" // Newer than baseTriggerTime (12:00:00Z)
        val serverProfile = CloudProfileDto(
            id = "user-1",
            displayName = "Hero",
            currentStreak = 5,
            longestStreak = 10,
            level = 3,
            totalXp = 500,
            leaderboardOptIn = true,
            updatedAt = newerServerTime
        )

        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 5,
            localLongestStreak = 10,
            localLevel = 3,
            localTotalXp = 500,
            localTriggerTime = baseTriggerTime,
            serverProfile = serverProfile
        )

        assertTrue(decision is SyncDecision.SkipServerNewer)
        val skip = decision as SyncDecision.SkipServerNewer
        assertEquals(newerServerTime, skip.serverUpdatedAt)
    }

    @Test
    fun evaluate_whenLocalIsNewerAndEqualOrHigherProgress_returnsPushLocal() {
        val olderServerTime = "2026-09-12T11:00:00Z" // Older than baseTriggerTime (12:00:00Z)
        val serverProfile = CloudProfileDto(
            id = "user-1",
            displayName = "Hero",
            currentStreak = 5,
            longestStreak = 10,
            level = 3,
            totalXp = 500,
            leaderboardOptIn = true,
            updatedAt = olderServerTime
        )

        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 6, // Incremented progress
            localLongestStreak = 10,
            localLevel = 3,
            localTotalXp = 550,
            localTriggerTime = baseTriggerTime,
            serverProfile = serverProfile
        )

        assertEquals(SyncDecision.PushLocal, decision)
    }

    @Test
    fun evaluate_whenServerTimestampIsInvalid_fallsBackToPushLocalIfProgressValid() {
        val serverProfile = CloudProfileDto(
            id = "user-1",
            displayName = "Hero",
            currentStreak = 5,
            longestStreak = 10,
            level = 3,
            totalXp = 500,
            leaderboardOptIn = true,
            updatedAt = "invalid-date-string"
        )

        val decision = SyncConflictResolver.evaluate(
            localCurrentStreak = 5,
            localLongestStreak = 10,
            localLevel = 3,
            localTotalXp = 500,
            localTriggerTime = baseTriggerTime,
            serverProfile = serverProfile
        )

        assertEquals(SyncDecision.PushLocal, decision)
    }
}
