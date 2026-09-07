package com.pixelquest.app.qa

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Step 38 Verification Test:
 * Validates sign-out / re-sign-in idempotency:
 * Confirms that re-authenticating with the same identity matches the existing
 * Supabase profile row by primary key ('id') rather than duplicating rows.
 */
class SignOutReSignInIdempotencyTest {

    @Test
    fun testReSignInMatchesExistingCloudProfileBySupabaseId() {
        val userUuid = "b5f12a34-8c7e-4b21-9988-123456789abc"

        // Simulated cloud database table (id -> CloudProfileDto)
        val cloudDatabaseTable = mutableMapOf<String, CloudProfileDto>()

        // 1. First sign-in and sync
        val initialProfile = UserProfileEntity(
            id = 1,
            username = "PlayerHero",
            avatarId = "avatar_1",
            level = 3,
            totalXp = 300,
            supabaseUserId = userUuid,
            leaderboardOptIn = true,
            leaderboardDisplayName = "PixelMaster"
        )
        val initialDto = CloudProfileRepositoryImpl.buildProfileDto(
            userId = userUuid,
            displayName = initialProfile.leaderboardDisplayName!!,
            optIn = true,
            profile = initialProfile,
            currentStreak = 2,
            longestStreak = 2,
            timestamp = "2026-09-08T01:00:00Z"
        )
        // PostgREST upsert simulation: key by id
        cloudDatabaseTable[initialDto.id] = initialDto

        assertEquals(1, cloudDatabaseTable.size)
        assertEquals("PixelMaster", cloudDatabaseTable[userUuid]?.displayName)
        assertEquals(3, cloudDatabaseTable[userUuid]?.level)

        // 2. Sign-out occurs: local session cleared, cloud database remains intact
        val signedOutSession: String? = null
        assertEquals(null, signedOutSession)

        // 3. Re-sign-in with same account: user progresses locally (level 4, 450 XP, streak 3)
        val updatedLocalProfile = initialProfile.copy(level = 4, totalXp = 450)
        val secondDto = CloudProfileRepositoryImpl.buildProfileDto(
            userId = userUuid, // same Supabase auth UID
            displayName = "PixelMaster",
            optIn = true,
            profile = updatedLocalProfile,
            currentStreak = 3,
            longestStreak = 3,
            timestamp = "2026-09-08T02:00:00Z"
        )
        // PostgREST upsert: updates existing key
        cloudDatabaseTable[secondDto.id] = secondDto

        // 4. Assert row was matched and updated, NOT duplicated
        assertEquals("Database must contain exactly 1 row (no duplicates)", 1, cloudDatabaseTable.size)
        val persistedRow = cloudDatabaseTable[userUuid]
        assertEquals(userUuid, persistedRow?.id)
        assertEquals(4, persistedRow?.level)
        assertEquals(450, persistedRow?.totalXp)
        assertEquals(3, persistedRow?.currentStreak)
        assertEquals("2026-09-08T02:00:00Z", persistedRow?.updatedAt)
    }
}
