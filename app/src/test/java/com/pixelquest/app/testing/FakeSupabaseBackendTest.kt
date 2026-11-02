package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.model.CloudProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FakeSupabaseBackendTest {

    private val backend = FakeSupabaseBackend()

    @Test
    fun anonUser_cannotReadProfiles() {
        backend.authenticate(null)
        try {
            backend.getProfile("user-1")
            fail("Expected SecurityException for anonymous user")
        } catch (expected: SecurityException) {
            assertTrue(expected.message?.contains("Anonymous users cannot read profiles") == true)
        }
    }

    @Test
    fun userCannotReadAnotherUsersPrivateProfile() {
        val user1 = "alice-uuid"
        val user2 = "bob-uuid"

        // Bob has a private profile (opted out of leaderboard)
        backend.authenticate(user2)
        backend.upsertProfile(
            CloudProfileDto(
                id = user2,
                displayName = "SecretBob",
                leaderboardOptIn = false,
                currentStreak = 10,
            ),
        )

        // Alice authenticates and attempts to read Bob's profile
        backend.authenticate(user1)
        val readProfile = backend.getProfile(user2)

        // RLS filters it out
        assertNull(readProfile)
    }

    @Test
    fun userCanReadAnotherUsersPublicProfile() {
        val user1 = "alice-uuid"
        val user2 = "bob-uuid"

        // Bob opts into leaderboard
        backend.authenticate(user2)
        backend.upsertProfile(
            CloudProfileDto(
                id = user2,
                displayName = "PublicBob",
                leaderboardOptIn = true,
                currentStreak = 15,
            ),
        )

        // Alice can read Bob's public profile
        backend.authenticate(user1)
        val readProfile = backend.getProfile(user2)
        assertNotNull(readProfile)
        assertEquals("PublicBob", readProfile?.displayName)
    }

    @Test
    fun userCanReadTheirOwnPrivateProfile() {
        val user1 = "alice-uuid"
        backend.authenticate(user1)
        backend.upsertProfile(
            CloudProfileDto(
                id = user1,
                displayName = "PrivateAlice",
                leaderboardOptIn = false,
                currentStreak = 5,
            ),
        )

        val readProfile = backend.getProfile(user1)
        assertNotNull(readProfile)
        assertEquals("PrivateAlice", readProfile?.displayName)
    }

    @Test
    fun userCannotWriteAnotherUsersProfile() {
        val user1 = "alice-uuid"
        val user2 = "bob-uuid"

        backend.authenticate(user1)
        try {
            backend.upsertProfile(
                CloudProfileDto(
                    id = user2,
                    displayName = "HackedBob",
                    leaderboardOptIn = true,
                ),
            )
            fail("Expected SecurityException when writing another user's row")
        } catch (expected: SecurityException) {
            assertTrue(expected.message?.contains("Cannot insert or update another user's profile row") == true)
        }
    }

    @Test
    fun userCannotReadAnotherUsersUsername() {
        val user1 = "alice-uuid"
        val user2 = "bob-uuid"

        backend.authenticate(user2)
        backend.setUsername(user2, "darood_bob")

        backend.authenticate(user1)
        try {
            backend.getUsername(user2)
            fail("Expected SecurityException when reading another user's private username")
        } catch (expected: SecurityException) {
            assertTrue(expected.message?.contains("One user cannot read another's private username row") == true)
        }
    }

    @Test
    fun nonMember_cannotReadPartyMembers() {
        val user1 = "alice-uuid"
        val user2 = "bob-uuid"
        val partyId = "secret-party"

        backend.authenticate(user1)
        backend.addPartyMember(partyId, user1)

        backend.authenticate(user2)
        try {
            backend.getPartyMembers(partyId)
            fail("Expected SecurityException when non-member reads party members")
        } catch (expected: SecurityException) {
            assertTrue(expected.message?.contains("is not an active member") == true)
        }
    }
}
