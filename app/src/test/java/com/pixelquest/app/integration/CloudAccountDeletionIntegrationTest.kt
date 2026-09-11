package com.pixelquest.app.integration

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.account.AccountViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudAccountDeletionIntegrationTest {

    @Test
    fun cloudAccountDeletion_removesCloudData_andPreservesLocalQuests() = runTest {
        // In-memory mock cloud database
        val cloudDatabase = mutableMapOf<String, CloudProfileDto>()
        var authUserDeleted = false

        val testUserId = "user-delete-test-uuid-777"
        val testDisplayName = "PurgeTarget_99"

        // Seed initial cloud profile
        cloudDatabase[testUserId] = CloudProfileDto(
            id = testUserId,
            displayName = testDisplayName,
            currentStreak = 15,
            longestStreak = 20,
            level = 7,
            totalXp = 2100,
            leaderboardOptIn = true
        )

        // Mock CloudProfileRepository
        val fakeCloudRepo = object : CloudProfileRepository {
            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
                return SupabaseResult.Success(Unit)
            }
            override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
                return SupabaseResult.Success(Unit)
            }
            override suspend fun deleteCloudProfile(): SupabaseResult<Unit> {
                cloudDatabase.remove(testUserId)
                return SupabaseResult.Success(Unit)
            }
        }

        // Mock AuthRepository
        val fakeAuthRepo = object : AuthRepository {
            override val currentUser: Flow<AuthUser?> = flowOf(AuthUser(testUserId, "hero@test.com", "Test Hero"))
            override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?): SupabaseResult<AuthUser> =
                SupabaseResult.Success(AuthUser(testUserId, "hero@test.com", "Test Hero"))
            override suspend fun signOut(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
            override suspend fun getInitialUser(): AuthUser? = AuthUser(testUserId, "hero@test.com", "Test Hero")
            override suspend fun deleteAccount(): SupabaseResult<Unit> {
                authUserDeleted = true
                return SupabaseResult.Success(Unit)
            }
        }

        // Local in-memory Room profile store
        val localProfileFlow = MutableStateFlow(
            UserProfileEntity(
                id = 1,
                username = "HeroWarrior",
                avatarId = "warrior",
                level = 7,
                totalXp = 2100,
                perfectDaysTowardNextLevel = 2,
                supabaseUserId = testUserId,
                leaderboardOptIn = true,
                leaderboardDisplayName = testDisplayName
            )
        )

        val fakeUserRepo = object : UserProfileRepository {
            override fun getProfile(): Flow<UserProfileEntity?> = localProfileFlow
            override suspend fun insertProfile(profile: UserProfileEntity) { localProfileFlow.value = profile }
            override suspend fun updateProfile(profile: UserProfileEntity) { localProfileFlow.value = profile }
            override suspend fun performLevelUp(): UserProfileEntity? = null
            override suspend fun updateSupabaseUserId(userId: String?) {
                localProfileFlow.value = localProfileFlow.value.copy(supabaseUserId = userId)
            }
            override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
                localProfileFlow.value = localProfileFlow.value.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
            }
            override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
                localProfileFlow.value = localProfileFlow.value.copy(leaderboardOptIn = optIn)
            }
            override suspend fun clearCloudData() {
                localProfileFlow.value = localProfileFlow.value.copy(
                    supabaseUserId = null,
                    leaderboardOptIn = false,
                    leaderboardDisplayName = null
                )
            }
        }

        // Leaderboard repository reading from in-memory cloud database
        val fakeLeaderboardRepo = object : LeaderboardRepository {
            override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> =
                SupabaseResult.Success(cloudDatabase.values.filter { it.leaderboardOptIn })

            override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> =
                SupabaseResult.Success(cloudDatabase.values.filter { it.leaderboardOptIn }.sortedByDescending { it.currentStreak })

            override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> =
                SupabaseResult.Success(cloudDatabase.values.filter { it.leaderboardOptIn }.sortedByDescending { it.level })

            override suspend fun getCurrentUserRank(sortMode: LeaderboardSortMode, userId: String?): SupabaseResult<UserLeaderboardRank?> {
                val profile = userId?.let { cloudDatabase[it] }
                return if (profile != null && profile.leaderboardOptIn) {
                    SupabaseResult.Success(UserLeaderboardRank(1, profile))
                } else {
                    SupabaseResult.Success(null)
                }
            }
        }

        // 1. Verify user initially exists in cloud database and leaderboard
        val initialLeaderboard = fakeLeaderboardRepo.getTopByStreak().let { (it as SupabaseResult.Success).data }
        assertTrue("Leaderboard should contain user before deletion", initialLeaderboard.any { it.id == testUserId })

        // 2. Initialize AccountViewModel and execute cloud account deletion
        val accountViewModel = AccountViewModel(
            userProfileRepository = fakeUserRepo,
            cloudProfileRepository = fakeCloudRepo,
            authRepository = fakeAuthRepo
        )
        advanceUntilIdle()

        // Trigger deletion flow
        accountViewModel.confirmDeleteCloudAccount()
        advanceUntilIdle()

        // 3. Verify cloud row deleted
        assertFalse("Cloud database must no longer contain deleted user profile", cloudDatabase.containsKey(testUserId))
        assertTrue("Auth account deletion RPC must have been called", authUserDeleted)

        // 4. Verify user disappears from leaderboard queries
        val postDeleteLeaderboard = fakeLeaderboardRepo.getTopByStreak().let { (it as SupabaseResult.Success).data }
        assertFalse("Leaderboard must not contain deleted user", postDeleteLeaderboard.any { it.id == testUserId })

        val rankResult = fakeLeaderboardRepo.getCurrentUserRank(LeaderboardSortMode.STREAK, testUserId).let {
            (it as SupabaseResult.Success).data
        }
        assertNull("Rank result for deleted user must be null", rankResult)

        // 5. Verify local Room progress is preserved (level 7, 2100 XP, warrior avatar intact)
        val finalLocalProfile = localProfileFlow.value
        assertEquals("Local hero level must remain intact", 7, finalLocalProfile.level)
        assertEquals("Local XP must remain intact", 2100, finalLocalProfile.totalXp)
        assertEquals("Local avatar class must remain intact", "warrior", finalLocalProfile.avatarId)
        assertNull("Local supabaseUserId must be cleared", finalLocalProfile.supabaseUserId)
        assertFalse("Local leaderboardOptIn must be false", finalLocalProfile.leaderboardOptIn)
        assertNull("Local leaderboardDisplayName must be cleared", finalLocalProfile.leaderboardDisplayName)
    }
}
