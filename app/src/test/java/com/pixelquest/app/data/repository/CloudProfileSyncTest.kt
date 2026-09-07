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
}
