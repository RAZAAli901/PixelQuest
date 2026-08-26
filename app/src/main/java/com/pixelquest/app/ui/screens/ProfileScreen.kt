package com.pixelquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.PixelAvatarDisplay
import com.pixelquest.app.ui.components.PixelAvatarFrame
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelXpBar
import com.pixelquest.app.ui.screens.profile.ProfileViewModel
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.PixelSurface
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

import androidx.compose.foundation.clickable

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToAvatarSelection: () -> Unit = {},
    onNavigateToDifficulty: () -> Unit = {},
    onNavigateToLevelHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile
    val streak = state.streak
    val difficulty = state.difficulty

    val username = profile?.username ?: "PixelHero"
    val level = profile?.level ?: 1
    val totalXp = profile?.totalXp ?: 0
    val streakCount = streak?.currentStreak ?: 0
    val diffLevel = difficulty?.difficultyLevel ?: DifficultyLevel.MEDIUM

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👤 HERO PROFILE",
            style = PixelTypography.titleLarge,
            color = PixelGold
        )

        val avatarId = profile?.avatarId ?: "avatar_hero"

        // Real Pixel Avatar Display with Level Tier Framing
        PixelAvatarFrame(
            avatarId = avatarId,
            level = level,
            size = 80.dp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onNavigateToAvatarSelection() }
        )

        // Profile Stats Overview Card
        PixelCard(
            variant = PixelPanelVariant.YELLOW,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = username.uppercase(),
                    style = PixelTypography.titleMedium,
                    color = PixelGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "LEVEL: $level", style = PixelTypography.bodyMedium, color = PixelTextWhite)
                    Text(text = "TOTAL XP: $totalXp", style = PixelTypography.bodyMedium, color = PixelGreen)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "STREAK: $streakCount DAYS", style = PixelTypography.bodyMedium, color = PixelCyan)
                    Text(text = "MODE: ${DifficultyMode.getDisplayName(diffLevel)}", style = PixelTypography.bodyMedium, color = PixelGold)
                }
            }
        }

        // Level XP Bar
        val perfectDays = profile?.perfectDaysTowardNextLevel ?: 0
        val daysRequired = difficulty?.daysRequiredPerLevel ?: 7
        PixelXpBar(
            currentProgress = perfectDays,
            maxProgress = daysRequired,
            level = level,
            modifier = Modifier.fillMaxWidth()
        )

        PixelButton(
            text = "📜 LEVEL HISTORY",
            onClick = onNavigateToLevelHistory,
            variant = PixelButtonVariant.BLUE,
            modifier = Modifier.fillMaxWidth()
        )

        PixelButton(
            text = "🛡️ CHANGE DIFFICULTY",
            onClick = onNavigateToDifficulty,
            variant = PixelButtonVariant.YELLOW,
            modifier = Modifier.fillMaxWidth()
        )

        PixelButton(
            text = "⚙️ APP SETTINGS",
            onClick = onNavigateToSettings,
            variant = PixelButtonVariant.GREEN,
            modifier = Modifier.fillMaxWidth()
        )

        val soundText = if (state.isSoundEnabled) "🔊 SFX: ON" else "🔇 SFX: OFF"
        PixelButton(
            text = soundText,
            onClick = { viewModel.toggleSound(!state.isSoundEnabled) },
            variant = PixelButtonVariant.BLUE,
            modifier = Modifier.fillMaxWidth()
        )

        val crtText = if (state.isCrtEnabled) "📺 CRT FILTER: ON" else "📺 CRT FILTER: OFF"
        PixelButton(
            text = crtText,
            onClick = { viewModel.toggleCrt(!state.isCrtEnabled) },
            variant = PixelButtonVariant.YELLOW,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
