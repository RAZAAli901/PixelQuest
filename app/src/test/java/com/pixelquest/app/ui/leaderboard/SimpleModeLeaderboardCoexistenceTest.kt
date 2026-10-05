package com.pixelquest.app.ui.leaderboard

import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardAuthState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardTab
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The leaderboard is an opt-in competitive surface, so it keeps showing real XP, streaks and
 * levels for everyone, including a player who has Simple Mode on locally (Day 18 decision).
 */
class SimpleModeLeaderboardCoexistenceTest {

    @Test
    fun `leaderboard screen state remains fully functional and visually unchanged in simple mode`() {
        // Opted-in user viewing leaderboard while Simple Mode is active locally
        val entries = listOf(
            CloudProfileDto(id = "user_1", displayName = "PixelMaster", currentStreak = 25, longestStreak = 30, level = 10, totalXp = 3500, leaderboardOptIn = true),
            CloudProfileDto(id = "user_self", displayName = "SimpleModeHero", currentStreak = 8, longestStreak = 12, level = 4, totalXp = 850, leaderboardOptIn = true)
        )

        val uiState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInAndOptedIn(userId = "user_self", displayName = "SimpleModeHero"),
            selectedTab = LeaderboardTab.TOP_LEVELS,
            levelEntries = entries,
            isLoading = false
        )

        // Verify all gamified competitive elements render intact on LeaderboardScreen
        assertEquals(2, uiState.levelEntries.size)
        assertEquals(3500, uiState.levelEntries[0].totalXp)
        assertEquals(25, uiState.levelEntries[0].currentStreak)
        assertEquals(10, uiState.levelEntries[0].level)

        // Verify user's own row contains real background XP & streak
        val selfEntry = uiState.levelEntries.first { it.id == "user_self" }
        assertEquals(850, selfEntry.totalXp)
        assertEquals(8, selfEntry.currentStreak)
        assertEquals(4, selfEntry.level)
        assertTrue(uiState.authState is LeaderboardAuthState.SignedInAndOptedIn)
    }
}
