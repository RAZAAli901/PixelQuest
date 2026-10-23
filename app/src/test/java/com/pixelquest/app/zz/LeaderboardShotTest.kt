package com.pixelquest.app.zz

import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.RankedProfile
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardAuthState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardContent
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardUiState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardView
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class LeaderboardShotTest : ScratchShot() {
    private fun hero(id: String, streak: Int) = CloudProfileDto(id = id, displayName = id, currentStreak = streak, longestStreak = streak, leaderboardOptIn = true)
    private val you = hero("raza", 12)
    private val state = LeaderboardUiState(
        authState = LeaderboardAuthState.SignedInAndOptedIn(userId = you.id, displayName = you.displayName),
        streakEntries = listOf(hero("sara", 41), hero("bram", 33), hero("aria_k", 29), hero("cleo_r", 20), you, hero("dev", 9)),
        currentUserRank = UserLeaderboardRank(5, you),
        selectedView = LeaderboardView.TOP,
        lastUpdatedTimestamp = "10:32 AM",
        canLoadMore = false
    )

    private fun content() = @androidx.compose.runtime.Composable {
        PixelQuestTheme { LeaderboardContent(uiState = state, onTabSelected = {}, onLoadMore = {}, onRefresh = {}, onNavigateBack = {}, onNavigateToAccount = {}) }
    }

    @Test fun comic() = shot("leaderboard", PqSkin.Comic, content())
    @Test fun pixel() = shot("leaderboard", PqSkin.Pixel, content())
}
