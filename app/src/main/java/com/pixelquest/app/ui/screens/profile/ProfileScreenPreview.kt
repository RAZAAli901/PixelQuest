package com.pixelquest.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.screens.ProfileContent
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Preview(name = "Profile Screen - Gamified Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun ProfileScreenGamifiedPreview() {
    PixelQuestTheme {
        val gamifiedState = ProfileUiState(
            profile = UserProfileEntity(
                id = 1,
                username = "PixelHero",
                level = 5,
                totalXp = 1250,
                perfectDaysTowardNextLevel = 4,
                avatarId = "avatar_mage"
            ),
            streak = StreakEntity(
                id = 1,
                currentStreak = 12,
                longestStreak = 18,
                lastCompletedDate = null
            ),
            difficulty = DifficultySettingsEntity(
                id = 1,
                difficultyLevel = DifficultyLevel.MEDIUM,
                perfectDayThreshold = 0.7f,
                daysRequiredPerLevel = 7
            ),
            isSimpleMode = false,
            totalTasksCompleted = 45,
            activeTasksCount = 5
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
        ) {
            ProfileContent(
                state = gamifiedState,
                onNavigateToAvatarSelection = {},
                onNavigateToDifficulty = {},
                onNavigateToLevelHistory = {},
                onNavigateToSettings = {}
            )
        }
    }
}

@Preview(name = "Profile Screen - Simple Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun ProfileScreenSimpleModePreview() {
    PixelQuestTheme {
        val simpleState = ProfileUiState(
            profile = UserProfileEntity(
                id = 1,
                username = "PixelHero",
                level = 5,
                totalXp = 1250,
                perfectDaysTowardNextLevel = 4,
                avatarId = "avatar_mage"
            ),
            streak = StreakEntity(
                id = 1,
                currentStreak = 12,
                longestStreak = 18,
                lastCompletedDate = null
            ),
            difficulty = DifficultySettingsEntity(
                id = 1,
                difficultyLevel = DifficultyLevel.MEDIUM,
                perfectDayThreshold = 0.7f,
                daysRequiredPerLevel = 7
            ),
            isSimpleMode = true,
            totalTasksCompleted = 45,
            activeTasksCount = 5
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
        ) {
            ProfileContent(
                state = simpleState,
                onNavigateToAvatarSelection = {},
                onNavigateToDifficulty = {},
                onNavigateToLevelHistory = {},
                onNavigateToSettings = {}
            )
        }
    }
}

@Preview(name = "Profile Screen - Gamified vs Simple Side by Side", showBackground = true, widthDp = 760, heightDp = 740)
@Composable
fun ProfileScreenComparisonSideBySidePreview() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ProfileScreenGamifiedPreview()
        }
        Box(modifier = Modifier.weight(1f)) {
            ProfileScreenSimpleModePreview()
        }
    }
}
