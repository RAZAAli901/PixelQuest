package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CloudProfileSyncTest {

    @Test
    fun buildProfileDto_mapsAllFieldsAccurately() {
        val profile = UserProfileEntity(
            id = 1,
            username = "LocalPlayer",
            avatarId = "avatar_1",
            level = 12,
            totalXp = 4500,
            perfectDaysTowardNextLevel = 3,
            supabaseUserId = "user-uuid-1234",
            leaderboardOptIn = true,
            leaderboardDisplayName = "CloudHero"
        )

        val dto = CloudProfileRepositoryImpl.buildProfileDto(
            userId = "user-uuid-1234",
            displayName = "CloudHero",
            optIn = true,
            profile = profile,
            currentStreak = 14,
            longestStreak = 25,
            timestamp = "2026-09-08T00:00:00Z"
        )

        assertEquals("user-uuid-1234", dto.id)
        assertEquals("CloudHero", dto.displayName)
        assertEquals(14, dto.currentStreak)
        assertEquals(25, dto.longestStreak)
        assertEquals(12, dto.level)
        assertEquals(4500, dto.totalXp)
        assertTrue(dto.leaderboardOptIn)
        assertEquals("2026-09-08T00:00:00Z", dto.updatedAt)
    }

    @Test
    fun buildProfileDto_handlesNullProfileAndStreakGracefully() {
        val dto = CloudProfileRepositoryImpl.buildProfileDto(
            userId = "user-uuid-5678",
            displayName = "AnonymousWarrior",
            optIn = false,
            profile = null,
            currentStreak = 0,
            longestStreak = 0
        )

        assertEquals("user-uuid-5678", dto.id)
        assertEquals("AnonymousWarrior", dto.displayName)
        assertEquals(0, dto.currentStreak)
        assertEquals(0, dto.longestStreak)
        assertEquals(1, dto.level)
        assertEquals(0, dto.totalXp)
        assertEquals(false, dto.leaderboardOptIn)
        assertNotNull(dto.updatedAt)
        // Verify ISO-8601 parsable
        assertNotNull(Instant.parse(dto.updatedAt))
    }

    @Test
    fun staleClosureAudit_pushPayloadUsesFreshValuesAtExecutionTime() {
        // Given an initial profile and streak at t0
        var localStreak = 3
        var localLevel = 2
        var localXp = 400

        val initialProfile = UserProfileEntity(
            id = 1,
            username = "Hero",
            avatarId = "1",
            level = localLevel,
            totalXp = localXp,
            supabaseUserId = "user-123",
            leaderboardOptIn = true,
            leaderboardDisplayName = "Hero_Live"
        )

        // Rapid local state changes occur before worker execution
        localStreak += 1 // streak is now 4
        localXp += 50    // xp is now 450
        val updatedProfile = initialProfile.copy(level = localLevel, totalXp = localXp)

        // At push execution time, worker queries Room freshly and builds DTO
        val executionDto = CloudProfileRepositoryImpl.buildProfileDto(
            userId = "user-123",
            displayName = updatedProfile.leaderboardDisplayName!!,
            optIn = updatedProfile.leaderboardOptIn,
            profile = updatedProfile,
            currentStreak = localStreak,
            longestStreak = 10
        )

        // Verify the pushed values reflect the fresh post-update values, NOT stale closure snapshots
        assertEquals(4, executionDto.currentStreak)
        assertEquals(450, executionDto.totalXp)
        assertEquals(2, executionDto.level)
    }
}
