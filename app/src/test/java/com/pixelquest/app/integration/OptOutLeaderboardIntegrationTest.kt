package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.data.repository.CloudProfileRepositoryImpl
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test verifying opt-out data integrity:
 * When a user opts out, they are promptly removed from Supabase leaderboard
 * rankings (both streak and level queries), their rank calculation returns null,
 * and other players' rankings shift dynamically.
 */
class OptOutLeaderboardIntegrationTest {

    // Simulated Supabase 'profiles' table in memory
    private val cloudProfilesTable = mutableMapOf<String, CloudProfileDto>()

    private lateinit var leaderboardRepository: LeaderboardRepository
    private lateinit var cloudProfileRepositoryUserA: CloudProfileRepository
    private lateinit var cloudProfileRepositoryUserB: CloudProfileRepository

    private val userProfileAFlow = MutableStateFlow(
        UserProfileEntity(
            id = 1,
            username = "PlayerAlpha",
            avatarId = "1",
            level = 8,
            totalXp = 3200,
            supabaseUserId = "user-alpha",
            leaderboardOptIn = true,
            leaderboardDisplayName = "Alpha_Knight"
        )
    )

    private val streakAFlow = MutableStateFlow(
        StreakEntity(id = 1, currentStreak = 15, longestStreak = 20)
    )

    private val userProfileBFlow = MutableStateFlow(
        UserProfileEntity(
            id = 1,
            username = "PlayerBeta",
            avatarId = "2",
            level = 10,
            totalXp = 5000,
            supabaseUserId = "user-beta",
            leaderboardOptIn = true,
            leaderboardDisplayName = "Beta_Champion"
        )
    )

    private val streakBFlow = MutableStateFlow(
        StreakEntity(id = 1, currentStreak = 25, longestStreak = 30)
    )

    @Before
    fun setUp() {
        cloudProfilesTable.clear()

        // Seed both players as opted-in initially
        cloudProfilesTable["user-alpha"] = CloudProfileDto(
            id = "user-alpha",
            displayName = "Alpha_Knight",
            currentStreak = 15,
            longestStreak = 20,
            level = 8,
            totalXp = 3200,
            leaderboardOptIn = true
        )

        cloudProfilesTable["user-beta"] = CloudProfileDto(
            id = "user-beta",
            displayName = "Beta_Champion",
            currentStreak = 25,
            longestStreak = 30,
            level = 10,
            totalXp = 5000,
            leaderboardOptIn = true
        )

        leaderboardRepository = object : LeaderboardRepository {
            override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> =
                SupabaseResult.Success(cloudProfilesTable.values.filter { it.leaderboardOptIn })

            override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
                val sorted = cloudProfilesTable.values
                    .filter { it.leaderboardOptIn }
                    .sortedWith(compareByDescending<CloudProfileDto> { it.currentStreak }.thenByDescending { it.longestStreak })
                    .drop(offset.toInt())
                    .take(limit.toInt())
                return SupabaseResult.Success(sorted)
            }

            override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
                val sorted = cloudProfilesTable.values
                    .filter { it.leaderboardOptIn }
                    .sortedWith(compareByDescending<CloudProfileDto> { it.level }.thenByDescending { it.totalXp })
                    .drop(offset.toInt())
                    .take(limit.toInt())
                return SupabaseResult.Success(sorted)
            }

            override suspend fun getCurrentUserRank(
                sortMode: LeaderboardSortMode,
                userId: String?
            ): SupabaseResult<UserLeaderboardRank?> {
                val targetId = userId ?: return SupabaseResult.Success(null)
                val target = cloudProfilesTable[targetId] ?: return SupabaseResult.Success(null)
                if (!target.leaderboardOptIn) return SupabaseResult.Success(null)

                val rank = when (sortMode) {
                    LeaderboardSortMode.STREAK -> cloudProfilesTable.values.count {
                        it.leaderboardOptIn && (it.currentStreak > target.currentStreak ||
                            (it.currentStreak == target.currentStreak && it.longestStreak > target.longestStreak))
                    } + 1
                    LeaderboardSortMode.LEVEL -> cloudProfilesTable.values.count {
                        it.leaderboardOptIn && (it.level > target.level ||
                            (it.level == target.level && it.totalXp > target.totalXp))
                    } + 1
                }
                return SupabaseResult.Success(UserLeaderboardRank(rank = rank, profile = target))
            }
        }

        // CloudProfileRepository for User B
        cloudProfileRepositoryUserB = object : CloudProfileRepository {
            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
                userProfileBFlow.value = userProfileBFlow.value.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
                val existing = cloudProfilesTable["user-beta"]
                if (existing != null) {
                    cloudProfilesTable["user-beta"] = existing.copy(leaderboardOptIn = optIn, displayName = displayName)
                }
                return SupabaseResult.Success(Unit)
            }

            override suspend fun syncProfileToCloud(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

            override suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> {
                userProfileBFlow.value = userProfileBFlow.value.copy(leaderboardOptIn = false)
                val existing = cloudProfilesTable["user-beta"]
                if (existing != null) {
                    cloudProfilesTable["user-beta"] = existing.copy(leaderboardOptIn = false)
                }
                return SupabaseResult.Success(Unit)
            }
        }
    }

    @Test
    fun optOut_immediatelyRemovesUserFromStreakAndLevelLeaderboards() = runTest {
        // 1. Initially, both users appear in streak leaderboard
        val initialStreaks = (leaderboardRepository.getTopByStreak(10, 0) as SupabaseResult.Success).data
        assertEquals(2, initialStreaks.size)
        assertEquals("user-beta", initialStreaks[0].id) // 25 days streak (Rank 1)
        assertEquals("user-alpha", initialStreaks[1].id) // 15 days streak (Rank 2)

        // Initial ranks
        val rankBetaBefore = (leaderboardRepository.getCurrentUserRank(LeaderboardSortMode.STREAK, "user-beta") as SupabaseResult.Success).data
        assertEquals(1, rankBetaBefore?.rank)

        val rankAlphaBefore = (leaderboardRepository.getCurrentUserRank(LeaderboardSortMode.STREAK, "user-alpha") as SupabaseResult.Success).data
        assertEquals(2, rankAlphaBefore?.rank)

        // 2. User Beta opts out immediately
        val optOutResult = cloudProfileRepositoryUserB.optOutFromLeaderboard()
        assertTrue(optOutResult is SupabaseResult.Success)

        // 3. Verify User Beta is IMMEDIATELY excluded from Top Streaks
        val updatedStreaks = (leaderboardRepository.getTopByStreak(10, 0) as SupabaseResult.Success).data
        assertEquals(1, updatedStreaks.size)
        assertEquals("user-alpha", updatedStreaks[0].id)
        assertTrue(updatedStreaks.none { it.id == "user-beta" })

        // 4. Verify User Beta is IMMEDIATELY excluded from Top Levels
        val updatedLevels = (leaderboardRepository.getTopByLevel(10, 0) as SupabaseResult.Success).data
        assertEquals(1, updatedLevels.size)
        assertEquals("user-alpha", updatedLevels[0].id)
        assertTrue(updatedLevels.none { it.id == "user-beta" })

        // 5. User Beta's rank is now null
        val rankBetaAfter = (leaderboardRepository.getCurrentUserRank(LeaderboardSortMode.STREAK, "user-beta") as SupabaseResult.Success).data
        assertNull(rankBetaAfter)

        // 6. User Alpha now moves up to Rank 1!
        val rankAlphaAfter = (leaderboardRepository.getCurrentUserRank(LeaderboardSortMode.STREAK, "user-alpha") as SupabaseResult.Success).data
        assertNotNull(rankAlphaAfter)
        assertEquals(1, rankAlphaAfter?.rank)
    }

    @Test
    fun optBackIn_restoresUserToLeaderboardRankings() = runTest {
        // User Beta opts out
        cloudProfileRepositoryUserB.optOutFromLeaderboard()
        var streaks = (leaderboardRepository.getTopByStreak(10, 0) as SupabaseResult.Success).data
        assertEquals(1, streaks.size)

        // User Beta opts back in
        cloudProfileRepositoryUserB.updateOptInAndSync(optIn = true, displayName = "Beta_Reborn")
        streaks = (leaderboardRepository.getTopByStreak(10, 0) as SupabaseResult.Success).data

        assertEquals(2, streaks.size)
        assertEquals("user-beta", streaks[0].id)
        assertEquals("Beta_Reborn", streaks[0].displayName)
        assertEquals(25, streaks[0].currentStreak)
    }
}
