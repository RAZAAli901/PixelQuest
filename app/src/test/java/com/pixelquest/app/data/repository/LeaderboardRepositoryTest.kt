package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class FakeLeaderboardRepository : LeaderboardRepository {
    val database = mutableListOf<CloudProfileDto>()
    var failure: Exception? = null

    override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> {
        failure?.let { return SupabaseResult.NetworkError(it) }
        return SupabaseResult.Success(database.filter { it.leaderboardOptIn })
    }

    override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        failure?.let { return SupabaseResult.NetworkError(it) }
        val sorted = database
            .filter { it.leaderboardOptIn }
            .sortedWith(compareByDescending<CloudProfileDto> { it.currentStreak }.thenByDescending { it.longestStreak })
            .drop(offset.toInt())
            .take(limit.toInt())
        return SupabaseResult.Success(sorted)
    }

    override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        failure?.let { return SupabaseResult.NetworkError(it) }
        val sorted = database
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
        failure?.let { return SupabaseResult.NetworkError(it) }
        val targetId = userId ?: return SupabaseResult.Success(null)
        val targetProfile = database.firstOrNull { it.id == targetId }
        if (targetProfile == null || !targetProfile.leaderboardOptIn) {
            return SupabaseResult.Success(null)
        }

        val rank = when (sortMode) {
            LeaderboardSortMode.STREAK -> {
                val higherStreaks = database.count { it.leaderboardOptIn && it.currentStreak > targetProfile.currentStreak }
                val sameStreakHigherLongest = database.count {
                    it.leaderboardOptIn &&
                    it.currentStreak == targetProfile.currentStreak &&
                    it.longestStreak > targetProfile.longestStreak
                }
                higherStreaks + sameStreakHigherLongest + 1
            }
            LeaderboardSortMode.LEVEL -> {
                val higherLevels = database.count { it.leaderboardOptIn && it.level > targetProfile.level }
                val sameLevelHigherXp = database.count {
                    it.leaderboardOptIn &&
                    it.level == targetProfile.level &&
                    it.totalXp > targetProfile.totalXp
                }
                higherLevels + sameLevelHigherXp + 1
            }
        }
        return SupabaseResult.Success(UserLeaderboardRank(rank = rank, profile = targetProfile))
    }
}

class LeaderboardRepositoryTest {

    private lateinit var repository: FakeLeaderboardRepository

    @Before
    fun setUp() {
        repository = FakeLeaderboardRepository()
        // Seed test data with varying streaks and levels
        val profiles = listOf(
            CloudProfileDto(id = "user-1", displayName = "PlayerOne", currentStreak = 10, longestStreak = 20, level = 5, totalXp = 1200, leaderboardOptIn = true),
            CloudProfileDto(id = "user-2", displayName = "PlayerTwo", currentStreak = 25, longestStreak = 30, level = 10, totalXp = 5000, leaderboardOptIn = true),
            CloudProfileDto(id = "user-3", displayName = "PlayerThree", currentStreak = 15, longestStreak = 15, level = 8, totalXp = 3400, leaderboardOptIn = true),
            CloudProfileDto(id = "user-4", displayName = "PlayerFour", currentStreak = 5, longestStreak = 10, level = 10, totalXp = 5200, leaderboardOptIn = true),
            CloudProfileDto(id = "user-5", displayName = "OptedOutPlayer", currentStreak = 50, longestStreak = 50, level = 20, totalXp = 9999, leaderboardOptIn = false),
            CloudProfileDto(id = "user-6", displayName = "PlayerSix", currentStreak = 2, longestStreak = 5, level = 2, totalXp = 300, leaderboardOptIn = true)
        )
        repository.database.addAll(profiles)
    }

    @Test
    fun getTopByStreak_sortsByCurrentStreakDescending_andExcludesOptedOut() = runTest {
        val result = repository.getTopByStreak(limit = 10, offset = 0)
        assertTrue(result is SupabaseResult.Success)
        val list = (result as SupabaseResult.Success).data

        // User 5 has streak 50 but opted out; should not appear
        assertTrue(list.none { it.id == "user-5" })

        // Expected order: user-2 (25), user-3 (15), user-1 (10), user-4 (5), user-6 (2)
        assertEquals(5, list.size)
        assertEquals("user-2", list[0].id)
        assertEquals(25, list[0].currentStreak)
        assertEquals("user-3", list[1].id)
        assertEquals(15, list[1].currentStreak)
        assertEquals("user-1", list[2].id)
        assertEquals(10, list[2].currentStreak)
        assertEquals("user-4", list[3].id)
        assertEquals(5, list[3].currentStreak)
        assertEquals("user-6", list[4].id)
        assertEquals(2, list[4].currentStreak)
    }

    @Test
    fun getTopByLevel_sortsByLevelDescending_withTotalXpTiebreaker() = runTest {
        val result = repository.getTopByLevel(limit = 10, offset = 0)
        assertTrue(result is SupabaseResult.Success)
        val list = (result as SupabaseResult.Success).data

        // Both user-4 and user-2 are level 10; user-4 has 5200 XP vs user-2 5000 XP
        assertEquals("user-4", list[0].id)
        assertEquals(10, list[0].level)
        assertEquals(5200, list[0].totalXp)

        assertEquals("user-2", list[1].id)
        assertEquals(10, list[1].level)
        assertEquals(5000, list[1].totalXp)

        assertEquals("user-3", list[2].id)
        assertEquals(8, list[2].level)

        assertEquals("user-1", list[3].id)
        assertEquals(5, list[3].level)
    }

    @Test
    fun pagination_offsetAndLimit_slicesResultsAccurately() = runTest {
        // Fetch page 1 (size 2)
        val page1 = repository.getTopByStreak(limit = 2, offset = 0) as SupabaseResult.Success
        assertEquals(2, page1.data.size)
        assertEquals("user-2", page1.data[0].id)
        assertEquals("user-3", page1.data[1].id)

        // Fetch page 2 (offset 2, size 2)
        val page2 = repository.getTopByStreak(limit = 2, offset = 2) as SupabaseResult.Success
        assertEquals(2, page2.data.size)
        assertEquals("user-1", page2.data[0].id)
        assertEquals("user-4", page2.data[1].id)

        // Fetch page 3 (offset 4, size 2)
        val page3 = repository.getTopByStreak(limit = 2, offset = 4) as SupabaseResult.Success
        assertEquals(1, page3.data.size)
        assertEquals("user-6", page3.data[0].id)
    }

    @Test
    fun getCurrentUserRank_computesRankCorrectlyEvenOutsideTopN() = runTest {
        // User 6 is rank 5 on streak leaderboard (out of 5 opted-in users)
        val rankResult = repository.getCurrentUserRank(LeaderboardSortMode.STREAK, userId = "user-6")
        assertTrue(rankResult is SupabaseResult.Success)
        val rankData = (rankResult as SupabaseResult.Success).data
        assertNotNull(rankData)
        assertEquals(5, rankData!!.rank)
        assertEquals("PlayerSix", rankData.profile.displayName)

        // User 2 is rank 1 on streak leaderboard
        val rank1 = repository.getCurrentUserRank(LeaderboardSortMode.STREAK, userId = "user-2") as SupabaseResult.Success
        assertEquals(1, rank1.data!!.rank)
    }

    @Test
    fun getCurrentUserRank_returnsNullForOptedOutUser() = runTest {
        val rankResult = repository.getCurrentUserRank(LeaderboardSortMode.STREAK, userId = "user-5")
        assertTrue(rankResult is SupabaseResult.Success)
        assertNull((rankResult as SupabaseResult.Success).data)
    }

    @Test
    fun getCurrentUserRank_levelModeTiebreaker_computesAccurateRank() = runTest {
        // user-4 (level 10, 5200 XP) -> rank 1
        val rankUser4 = repository.getCurrentUserRank(LeaderboardSortMode.LEVEL, userId = "user-4") as SupabaseResult.Success
        assertEquals(1, rankUser4.data!!.rank)

        // user-2 (level 10, 5000 XP) -> rank 2
        val rankUser2 = repository.getCurrentUserRank(LeaderboardSortMode.LEVEL, userId = "user-2") as SupabaseResult.Success
        assertEquals(2, rankUser2.data!!.rank)
    }

    @Test
    fun errorHandling_mapsFailureToNetworkError() = runTest {
        repository.failure = IOException("Network connection lost")
        val result = repository.getTopByStreak(10, 0)
        assertTrue(result is SupabaseResult.NetworkError)
        val error = result as SupabaseResult.NetworkError
        assertTrue(error.userMessage.contains("Network error", ignoreCase = true))
    }
}
