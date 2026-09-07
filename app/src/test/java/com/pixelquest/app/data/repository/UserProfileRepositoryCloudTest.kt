package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.UserProfileDao
import com.pixelquest.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeUserProfileDao : UserProfileDao {
    val profileFlow = MutableStateFlow<UserProfileEntity?>(null)

    override suspend fun insertProfile(profile: UserProfileEntity) {
        profileFlow.value = profile
    }

    override suspend fun updateProfile(profile: UserProfileEntity) {
        profileFlow.value = profile
    }

    override fun getProfile(): Flow<UserProfileEntity?> = profileFlow

    override suspend fun updateSupabaseUserId(userId: String?) {
        profileFlow.value = profileFlow.value?.copy(supabaseUserId = userId)
    }

    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
        profileFlow.value = profileFlow.value?.copy(
            leaderboardOptIn = optIn,
            leaderboardDisplayName = displayName
        )
    }

    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
        profileFlow.value = profileFlow.value?.copy(leaderboardOptIn = optIn)
    }
}

class UserProfileRepositoryCloudTest {

    private lateinit var fakeDao: FakeUserProfileDao
    private lateinit var repository: UserProfileRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeUserProfileDao()
        repository = UserProfileRepositoryImpl(fakeDao)
    }

    @Test
    fun defaultProfile_hasNullCloudIdAndOptInFalse() = runBlocking {
        fakeDao.insertProfile(
            UserProfileEntity(id = 1, username = "PixelHero", avatarId = "avatar_1")
        )

        val profile = repository.getProfile().first()
        assertNotNull(profile)
        assertNull(profile?.supabaseUserId)
        assertFalse(profile?.leaderboardOptIn ?: true)
        assertNull(profile?.leaderboardDisplayName)
    }

    @Test
    fun updateSupabaseUserId_updatesStoredCloudId() = runBlocking {
        fakeDao.insertProfile(
            UserProfileEntity(id = 1, username = "PixelHero", avatarId = "avatar_1")
        )

        repository.updateSupabaseUserId("supabase_user_xyz")
        val updated = repository.getProfile().first()
        assertEquals("supabase_user_xyz", updated?.supabaseUserId)
    }

    @Test
    fun updateLeaderboardSettings_updatesOptInAndDisplayName() = runBlocking {
        fakeDao.insertProfile(
            UserProfileEntity(id = 1, username = "PixelHero", avatarId = "avatar_1")
        )

        repository.updateLeaderboardSettings(optIn = true, displayName = "KnightRider")
        val updated = repository.getProfile().first()
        assertTrue(updated?.leaderboardOptIn == true)
        assertEquals("KnightRider", updated?.leaderboardDisplayName)
        assertEquals("PixelHero", updated?.username)
    }

    @Test
    fun updateLeaderboardOptIn_togglesOptInWithoutAffectingDisplayName() = runBlocking {
        fakeDao.insertProfile(
            UserProfileEntity(
                id = 1,
                username = "PixelHero",
                avatarId = "avatar_1",
                leaderboardOptIn = true,
                leaderboardDisplayName = "KnightRider"
            )
        )

        repository.updateLeaderboardOptIn(false)
        val updated = repository.getProfile().first()
        assertFalse(updated?.leaderboardOptIn ?: true)
        assertEquals("KnightRider", updated?.leaderboardDisplayName)
    }
}
