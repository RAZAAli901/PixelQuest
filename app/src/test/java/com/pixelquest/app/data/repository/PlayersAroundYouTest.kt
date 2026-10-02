package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AROUND YOU view: the heroes just above and below the signed-in user, with real ranks.
 */
class PlayersAroundYouTest {

    /**
     * In-memory leaderboard with the same ordering as LeaderboardRepositoryImpl: first column desc,
     * second column desc, then id asc for exact ties; rank is the position in that order.
     */
    private class InMemoryLeaderboard(private val profiles: List<CloudProfileDto>) : LeaderboardRepository {
        var failRank = false

        private fun ordered(mode: LeaderboardSortMode) = profiles.filter { it.leaderboardOptIn }.sortedWith(
            when (mode) {
                LeaderboardSortMode.STREAK -> compareByDescending<CloudProfileDto> { it.currentStreak }.thenByDescending { it.longestStreak }
                LeaderboardSortMode.LEVEL -> compareByDescending<CloudProfileDto> { it.level }.thenByDescending { it.totalXp }
            }.thenBy { it.id }
        )

        private fun page(mode: LeaderboardSortMode, limit: Long, offset: Long) =
            SupabaseResult.Success(ordered(mode).drop(offset.toInt()).take(limit.toInt()))

        override suspend fun getProfiles() = SupabaseResult.Success(profiles)
        override suspend fun getTopByStreak(limit: Long, offset: Long) = page(LeaderboardSortMode.STREAK, limit, offset)
        override suspend fun getTopByLevel(limit: Long, offset: Long) = page(LeaderboardSortMode.LEVEL, limit, offset)
        override suspend fun getCurrentUserRank(sortMode: LeaderboardSortMode, userId: String?): SupabaseResult<UserLeaderboardRank?> {
            if (failRank) return SupabaseResult.NetworkError(RuntimeException("offline"))
            val list = ordered(sortMode)
            val index = list.indexOfFirst { it.id == userId }
            return SupabaseResult.Success(if (index < 0) null else UserLeaderboardRank(index + 1, list[index]))
        }
    }

    private fun hero(id: String, streak: Int, longest: Int = streak, optIn: Boolean = true) =
        CloudProfileDto(id = id, displayName = "Hero_$id", currentStreak = streak, longestStreak = longest, leaderboardOptIn = optIn)

    /** Ten heroes with streaks 100, 90, ... 10: hero "h1" is 1st, "h10" is 10th. */
    private val ten = (1..10).map { hero("h%02d".format(it), streak = 110 - it * 10) }

    private suspend fun around(repo: LeaderboardRepository, userId: String) =
        (repo.getPlayersAroundYou(LeaderboardSortMode.STREAK, userId, radius = 3) as SupabaseResult.Success).data

    @Test
    fun middleOfTheBoard_showsThreeAboveAndThreeBelow() = runBlocking {
        val result = around(InMemoryLeaderboard(ten), "h06")!!

        assertEquals(6, result.you.rank)
        assertEquals(listOf(3, 4, 5, 6, 7, 8, 9), result.entries.map { it.rank })
        assertEquals("h06", result.entries.single { it.rank == 6 }.profile.id)
    }

    @Test
    fun firstPlace_showsOnlyHeroesBelow() = runBlocking {
        val result = around(InMemoryLeaderboard(ten), "h01")!!

        assertEquals(listOf(1, 2, 3, 4), result.entries.map { it.rank })
        assertEquals("h01", result.entries.first().profile.id)
    }

    @Test
    fun lastPlace_showsOnlyHeroesAbove() = runBlocking {
        val result = around(InMemoryLeaderboard(ten), "h10")!!

        assertEquals(listOf(7, 8, 9, 10), result.entries.map { it.rank })
        assertEquals("h10", result.entries.last().profile.id)
    }

    @Test
    fun exactTies_stillPlaceYouInsideTheWindow() = runBlocking {
        // Eight heroes on the same streak: order falls back to id, and every rank is distinct.
        val tied = (1..8).map { hero("t$it", streak = 5) }
        val result = around(InMemoryLeaderboard(tied), "t8")!!

        assertEquals(8, result.you.rank)
        assertEquals(listOf(5, 6, 7, 8), result.entries.map { it.rank })
        assertTrue(result.entries.any { it.profile.id == "t8" })
    }

    @Test
    fun notOnTheBoard_returnsNull() = runBlocking {
        val withSpectator = ten + hero("watcher", streak = 50, optIn = false)

        assertNull(around(InMemoryLeaderboard(withSpectator), "watcher"))
    }

    @Test
    fun rankFailure_isPassedThrough() = runBlocking {
        val repo = InMemoryLeaderboard(ten).apply { failRank = true }

        assertTrue(repo.getPlayersAroundYou(LeaderboardSortMode.STREAK, "h05") is SupabaseResult.NetworkError)
    }

    @Test
    fun windowMaths() {
        assertEquals(0L, LeaderboardWindow.offset(rank = 1, radius = 3))
        assertEquals(4L, LeaderboardWindow.limit(rank = 1, radius = 3))
        assertEquals(2L, LeaderboardWindow.offset(rank = 6, radius = 3))
        assertEquals(7L, LeaderboardWindow.limit(rank = 6, radius = 3))
        assertEquals(listOf(3, 4), LeaderboardWindow.ranked(listOf(hero("a", 1), hero("b", 1)), offset = 2).map { it.rank })
    }
}
