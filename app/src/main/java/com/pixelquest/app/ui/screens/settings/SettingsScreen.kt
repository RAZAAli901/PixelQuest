package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToDifficulty: () -> Unit = {},
    onNavigateToAvatar: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToThemeSelection: () -> Unit = {},
    onResetComplete: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackupToUri(context, it) }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onImportFileSelected(context, it) }
    }

    SettingsScreenScaffold(
        accountSection = {
            val username = state.profile?.username ?: "PixelHero"
            com.pixelquest.app.ui.components.PixelTextField(
                value = username,
                onValueChange = { viewModel.updateUsername(it) },
                label = "EDIT HERO NAME",
                placeholder = "Enter username",
                modifier = Modifier.fillMaxWidth()
            )
            val avatarId = state.profile?.avatarId ?: "avatar_hero"
            val level = state.profile?.level ?: 1
            com.pixelquest.app.ui.components.PixelAvatarFrame(
                avatarId = avatarId,
                level = level,
                size = 64.dp
            )
            PixelButton(
                text = "🧙 CHANGE AVATAR",
                onClick = onNavigateToAvatar,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
            val diffLevel = state.difficulty?.difficultyLevel ?: com.pixelquest.app.domain.model.DifficultyLevel.MEDIUM
            val diffName = com.pixelquest.app.domain.DifficultyMode.getDisplayName(diffLevel)
            androidx.compose.material3.Text(
                text = "CURRENT DIFFICULTY: ${diffName.uppercase()}",
                style = com.pixelquest.app.ui.theme.PixelTypography.bodyMedium,
                color = com.pixelquest.app.ui.theme.PixelTheme.colors.primary
            )
            PixelButton(
                text = "🛡️ CHANGE DIFFICULTY",
                onClick = onNavigateToDifficulty,
                enabled = !state.isSimpleModeEnabled,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.isSimpleModeEnabled) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = "🔒 Difficulty selection is locked while Simple Mode is active. Thresholds and streaks are paused.",
                    style = com.pixelquest.app.ui.theme.PixelTypography.labelSmall,
                    color = com.pixelquest.app.ui.theme.PixelTheme.colors.onSurfaceVariant
                )
            }
            PixelButton(
                text = "☁️ CLOUD & LEADERBOARD",
                onClick = onNavigateToAccount,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
        },
        notificationsSection = {
            val notifText = if (state.isNotificationsEnabled) "🔔 NOTIFICATIONS: ON" else "🔕 NOTIFICATIONS: OFF"
            PixelButton(
                text = notifText,
                onClick = { viewModel.toggleNotifications(!state.isNotificationsEnabled) },
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )
            PixelButton(
                text = "⚙️ OS NOTIFICATION SETTINGS",
                onClick = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
        },
        appearanceSection = {
            ThemeSelectionCard(
                currentTheme = state.themeMode,
                onThemeSelected = { viewModel.setThemeMode(it) },
                modifier = Modifier.fillMaxWidth()
            )
            PixelButton(
                text = "🎨 FULL THEME SETTINGS",
                onClick = onNavigateToThemeSelection,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )
            val soundText = if (state.isSoundEnabled) "🔊 SFX: ON" else "🔇 SFX: OFF"
            PixelButton(
                text = soundText,
                onClick = { viewModel.toggleSound(!state.isSoundEnabled) },
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
            val hapticText = if (state.isHapticsEnabled) "📳 HAPTICS: ON" else "📴 HAPTICS: OFF"
            PixelButton(
                text = hapticText,
                onClick = { viewModel.toggleHaptics(!state.isHapticsEnabled) },
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
        },
        simpleModeSection = {
            androidx.compose.material3.Text(
                text = "Minimalist, un-gamified task tracking. Hides XP, streaks, levels, and celebration modals while preserving all background progress.",
                style = com.pixelquest.app.ui.theme.PixelTypography.bodySmall,
                color = com.pixelquest.app.ui.theme.PixelTheme.colors.onSurfaceVariant
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = "MODE STATUS:",
                    style = com.pixelquest.app.ui.theme.PixelTypography.labelSmall,
                    color = com.pixelquest.app.ui.theme.PixelTheme.colors.primary
                )
                androidx.compose.material3.Text(
                    text = if (state.isSimpleModeEnabled) "✨ SIMPLE (ACTIVE)" else "⚔️ GAMIFIED (ACTIVE)",
                    style = com.pixelquest.app.ui.theme.PixelTypography.labelMedium,
                    color = if (state.isSimpleModeEnabled) com.pixelquest.app.ui.theme.PixelTheme.colors.secondary else com.pixelquest.app.ui.theme.PixelTheme.colors.tertiary
                )
            }
            val simpleModeButtonText = if (state.isSimpleModeEnabled) "📋 SIMPLE MODE: ON" else "📋 ENABLE SIMPLE MODE"
            PixelButton(
                text = simpleModeButtonText,
                onClick = {
                    if (!state.isSimpleModeEnabled) {
                        viewModel.requestEnableSimpleMode()
                    } else {
                        viewModel.toggleSimpleMode(false)
                    }
                },
                variant = if (state.isSimpleModeEnabled) PixelButtonVariant.YELLOW else PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.isSimpleModeEnabled) {
                PixelButton(
                    text = "🎮 SWITCH BACK TO FULL GAME MODE",
                    onClick = { viewModel.toggleSimpleMode(false) },
                    variant = PixelButtonVariant.YELLOW,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dataSection = {
            PixelButton(
                text = "📤 EXPORT QUEST DATA (JSON)",
                onClick = { exportLauncher.launch("pixelquest_backup.json") },
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )
            PixelButton(
                text = "📥 IMPORT QUEST DATA (JSON)",
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dangerZoneSection = {
            PixelButton(
                text = "🔥 RESET ALL PROGRESS",
                onClick = { viewModel.onResetProgressClicked() },
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )

    val showSimpleModeDialog by viewModel.showSimpleModeDialog.collectAsState()
    if (showSimpleModeDialog) {
        SimpleModeEnableDialog(
            onConfirm = { viewModel.confirmEnableSimpleMode() },
            onDismiss = { viewModel.dismissSimpleModeDialog() }
        )
    }

    val showRestoreDialog by viewModel.showRestoreConfirmDialog.collectAsState()
    if (showRestoreDialog) {
        RestoreDataConfirmDialog(
            onConfirm = { viewModel.confirmImport() },
            onDismiss = { viewModel.dismissImportDialog() }
        )
    }

    val resetStepState by viewModel.resetStep.collectAsState()
    if (resetStepState > 0) {
        ResetProgressDialogSequence(
            step = resetStepState,
            onNextStep = { viewModel.advanceResetStep() },
            onConfirmWipe = { viewModel.performFullReset(onResetComplete) },
            onDismiss = { viewModel.cancelReset() }
        )
    }
}
