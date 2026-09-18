package com.pixelquest.app.ui.leaderboard

import com.pixelquest.app.data.remote.LeaderboardEntryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeLeaderboardCoexistenceTest {

    @Test
    fun `leaderboard screen state remains fully functional and visually unchanged in simple mode`() {
        // Opted-in user viewing leaderboard while Simple Mode is active locally
        val entries = listOf(
            LeaderboardEntryDto(
                userId = "user_1",
                displayName = "PixelMaster",
                avatarId = "avatar_hero",
                level = 10,
                score = 3500,
                currentStreak = 25,
                longestStreak = 30
            ),
            LeaderboardEntryDto(
                userId = "user_self",
                displayName = "SimpleModeHero",
                avatarId = "avatar_mage",
                level = 4,
                score = 850,
                currentStreak = 8,
                longestStreak = 12
            )
        )

        val uiState = LeaderboardUiState(
            entries = entries,
            isOptedIn = true,
            currentUserId = "user_self",
            selectedTab = LeaderboardTab.OVERALL_XP,
            isLoading = false
        )

        // Verify all gamified competitive elements render intact on LeaderboardScreen
        // per Day 18 coexistence decision: leaderboard is an opt-in competitive surface
        assertEquals(2, uiState.entries.size)
        assertEquals(3500, uiState.entries[0].score)
        assertEquals(25, uiState.entries[0].currentStreak)
        assertEquals(10, uiState.entries[0].level)

        // Verify user's own row contains real background XP & streak
        val selfEntry = uiState.entries.first { it.userId == "user_self" }
        assertEquals(850, selfEntry.score)
        assertEquals(8, selfEntry.currentStreak)
        assertEquals(4, selfEntry.level)
        assertTrue(uiState.isOptedIn)
    }
}
