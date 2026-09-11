package com.pixelquest.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelQuestTheme

private val sampleStreakProfiles = listOf(
    CloudProfileDto(id = "p1", displayName = "Valkyrie99", currentStreak = 42, longestStreak = 42, level = 15, totalXp = 12000, leaderboardOptIn = true),
    CloudProfileDto(id = "p2", displayName = "ShadowKnight", currentStreak = 28, longestStreak = 35, level = 12, totalXp = 8500, leaderboardOptIn = true),
    CloudProfileDto(id = "p3", displayName = "PixelMage", currentStreak = 21, longestStreak = 25, level = 9, totalXp = 5400, leaderboardOptIn = true),
    CloudProfileDto(id = "user_hero", displayName = "ArcadeKing", currentStreak = 14, longestStreak = 14, level = 8, totalXp = 4200, leaderboardOptIn = true),
    CloudProfileDto(id = "p5", displayName = "RogueRider", currentStreak = 7, longestStreak = 10, level = 5, totalXp = 2100, leaderboardOptIn = true)
)

private val sampleLevelProfiles = listOf(
    CloudProfileDto(id = "p1", displayName = "Valkyrie99", currentStreak = 42, longestStreak = 42, level = 15, totalXp = 12000, leaderboardOptIn = true),
    CloudProfileDto(id = "p2", displayName = "ShadowKnight", currentStreak = 28, longestStreak = 35, level = 12, totalXp = 8500, leaderboardOptIn = true),
    CloudProfileDto(id = "p3", displayName = "PixelMage", currentStreak = 21, longestStreak = 25, level = 9, totalXp = 5400, leaderboardOptIn = true),
    CloudProfileDto(id = "user_hero", displayName = "ArcadeKing", currentStreak = 14, longestStreak = 14, level = 8, totalXp = 4200, leaderboardOptIn = true),
    CloudProfileDto(id = "p5", displayName = "RogueRider", currentStreak = 7, longestStreak = 10, level = 5, totalXp = 2100, leaderboardOptIn = true)
)

@Preview(name = "1. Not Signed In State", showBackground = true, backgroundColor = 0xFF12121E)
@Composable
fun LeaderboardPreview_NotSignedIn() {
    PixelQuestTheme {
        LeaderboardContent(
            uiState = LeaderboardUiState(
                authState = LeaderboardAuthState.NotSignedIn
            ),
            onTabSelected = {},
            onLoadMore = {},
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToAccount = {}
        )
    }
}

@Preview(name = "2. Signed In Read Only (Spectator)", showBackground = true, backgroundColor = 0xFF12121E)
@Composable
fun LeaderboardPreview_SignedInReadOnly() {
    PixelQuestTheme {
        LeaderboardContent(
            uiState = LeaderboardUiState(
                authState = LeaderboardAuthState.SignedInReadOnly(userId = "spectator_1"),
                selectedTab = LeaderboardTab.TOP_STREAKS,
                streakEntries = sampleStreakProfiles,
                canLoadMore = false
            ),
            onTabSelected = {},
            onLoadMore = {},
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToAccount = {}
        )
    }
}

@Preview(name = "3. Signed In & Opted In (Ranked & Pinned)", showBackground = true, backgroundColor = 0xFF12121E)
@Composable
fun LeaderboardPreview_SignedInAndOptedIn() {
    PixelQuestTheme {
        LeaderboardContent(
            uiState = LeaderboardUiState(
                authState = LeaderboardAuthState.SignedInAndOptedIn(
                    userId = "user_hero",
                    displayName = "ArcadeKing"
                ),
                selectedTab = LeaderboardTab.TOP_STREAKS,
                streakEntries = sampleStreakProfiles,
                currentUserRank = UserLeaderboardRank(
                    rank = 4,
                    profile = sampleStreakProfiles[3]
                ),
                canLoadMore = false
            ),
            onTabSelected = {},
            onLoadMore = {},
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToAccount = {}
        )
    }
}

@Preview(name = "4. Rank Tier Podium Top 3 Preview", showBackground = true, backgroundColor = 0xFF12121E)
@Composable
fun LeaderboardTop3PodiumPreview() {
    PixelQuestTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelBackgroundDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "👑 HALL OF FAME PODIUM",
                style = MaterialTheme.typography.titleMedium,
                color = PixelGold
            )
            PixelLeaderboardRow(
                rank = 1,
                displayName = "Valkyrie99",
                statLabel = "DAYS STREAK",
                statValue = "42 🔥",
                isCurrentUser = false
            )
            PixelLeaderboardRow(
                rank = 2,
                displayName = "ShadowKnight",
                statLabel = "DAYS STREAK",
                statValue = "28 🔥",
                isCurrentUser = false
            )
            PixelLeaderboardRow(
                rank = 3,
                displayName = "PixelMage",
                statLabel = "DAYS STREAK",
                statValue = "21 🔥",
                isCurrentUser = true
            )
        }
    }
}
