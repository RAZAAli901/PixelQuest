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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.PixelAvatarFrame
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelXpBar
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.screens.profile.ProfileViewModel
import com.pixelquest.app.ui.theme.PixelTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.clickable

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToAvatarSelection: () -> Unit = {},
    onNavigateToDifficulty: () -> Unit = {},
    onNavigateToLevelHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToUsername: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    ProfileContent(
        state = state,
        onNavigateToAvatarSelection = onNavigateToAvatarSelection,
        onNavigateToDifficulty = onNavigateToDifficulty,
        onNavigateToLevelHistory = onNavigateToLevelHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToUsername = onNavigateToUsername
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onNavigateToAvatarSelection: () -> Unit = {},
    onNavigateToDifficulty: () -> Unit = {},
    onNavigateToLevelHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToUsername: () -> Unit = {}
) {
    val profile = state.profile
    val streak = state.streak
    val difficulty = state.difficulty

    val username = profile?.username ?: "PixelHero"
    val level = profile?.level ?: 1
    val totalXp = profile?.totalXp ?: 0
    val streakCount = streak?.currentStreak ?: 0
    val diffLevel = difficulty?.difficultyLevel ?: DifficultyLevel.MEDIUM
    val colors = PixelTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)

            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // The side menu's three-line icon at the top left, then the title.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            com.pixelquest.app.ui.menu.MenuTopBarAction()
            Text(
                text = if (state.isSimpleMode) "👤 USER PROFILE" else "👤 HERO PROFILE",
                style = MaterialTheme.typography.titleLarge,
                color = colors.primaryText
            )
        }

        if (state.isSimpleMode) {
            PixelCard(
                variant = PixelPanelVariant.BEIGE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "📋 SIMPLE MODE ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary)
                    )
                }
            }
        }

        val avatarId = profile?.avatarId ?: "avatar_hero"

        // Real Pixel Avatar Display with Level Tier Framing (or neutral frame in Simple Mode)
        PixelAvatarFrame(
            avatarId = avatarId,
            level = level,
            size = 80.dp,
            isSimpleMode = state.isSimpleMode,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onNavigateToAvatarSelection() }
        )

        // Profile Stats Overview Card
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = username.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary)
                )
                // The Darood Party username, once the account has one: tap to change it.
                state.daroodUsername?.let { handle ->
                    val changeLabel = androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.darood_username_profile_change)
                    // TalkBack hears what the name is for, not just "at aria k".
                    val spoken = androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.darood_username_profile_label) + ", " +
                        androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.darood_username_handle, handle)
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.darood_username_handle, handle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClickLabel = changeLabel, onClick = onNavigateToUsername)
                            .semantics { contentDescription = spoken }
                            .testTag(com.pixelquest.app.darood.ui.DaroodTestTags.PROFILE_USERNAME)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!state.isSimpleMode) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "LEVEL: $level", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        Text(text = "TOTAL XP: $totalXp", style = MaterialTheme.typography.bodyMedium, color = com.pixelquest.app.ui.theme.inkOnPanel(colors.tertiary))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "STREAK: $streakCount DAYS", style = MaterialTheme.typography.bodyMedium, color = com.pixelquest.app.ui.theme.inkOnPanel(colors.secondary))
                        Text(text = "MODE: ${DifficultyMode.getDisplayName(diffLevel)}", style = MaterialTheme.typography.bodyMedium, color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary))
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "TASKS COMPLETED: ${state.totalTasksCompleted}", style = MaterialTheme.typography.bodyMedium, color = com.pixelquest.app.ui.theme.inkOnPanel(colors.tertiary))
                        Text(text = "ACTIVE TASKS: ${state.activeTasksCount}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                }
            }
        }

        // Level XP Bar
        if (!state.isSimpleMode) {
            val perfectDays = profile?.perfectDaysTowardNextLevel ?: 0
            val daysRequired = com.pixelquest.app.domain.DifficultyMode.daysRequiredPerLevel(difficulty)
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
        }

        PixelButton(
            text = "🛡️ CHANGE DIFFICULTY",
            onClick = onNavigateToDifficulty,
            enabled = !state.isSimpleMode,
            variant = PixelButtonVariant.YELLOW,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.isSimpleMode) {
            Text(
                text = "🔒 Difficulty selection is locked while Simple Mode is active.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }

        PixelButton(
            text = "⚙️ APP SETTINGS",
            onClick = onNavigateToSettings,
            variant = PixelButtonVariant.YELLOW,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
