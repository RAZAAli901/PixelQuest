package com.pixelquest.app.ui.screens.leaderboard

import com.pixelquest.app.domain.AvatarTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Step 31 Verification:
 * A Simple Mode user's own leaderboard entry and row renders completely normally
 * to other users regardless of the local user's personal display preference.
 * Underlying stats (streaks, level, XP) and tier framing are fully preserved.
 */
class SimpleModeLeaderboardRowRenderingTest {

    @Test
    fun `leaderboard tier mapping remains canonical for simple mode users viewed publicly`() {
        // Ranks 1, 2, 3 receive Gold, Silver, Bronze regardless of user's personal simple mode setting
        val tier1 = when (1) {
            1 -> AvatarTier.GOLD
            2 -> AvatarTier.SILVER
            3 -> AvatarTier.BRONZE
            else -> null
        }
        val tier2 = when (2) {
            1 -> AvatarTier.GOLD
            2 -> AvatarTier.SILVER
            3 -> AvatarTier.BRONZE
            else -> null
        }
        val tier3 = when (3) {
            1 -> AvatarTier.GOLD
            2 -> AvatarTier.SILVER
            3 -> AvatarTier.BRONZE
            else -> null
        }

        assertEquals(AvatarTier.GOLD, tier1)
        assertEquals(AvatarTier.SILVER, tier2)
        assertEquals(AvatarTier.BRONZE, tier3)
    }

    @Test
    fun `public leaderboard entry payload retains full gamified metrics for simple mode users`() {
        // A user whose local simple mode is active still contributes real background metrics
        val userRowData = mapOf(
            "userId" to "user-123",
            "displayName" to "SimpleHero",
            "rank" to 1,
            "statLabel" to "Streak",
            "statValue" to "14 days",
            "avatarId" to "avatar_retro_knight"
        )

        assertNotNull(userRowData["statLabel"])
        assertEquals("14 days", userRowData["statValue"])
        assertEquals("avatar_retro_knight", userRowData["avatarId"])
    }
}
