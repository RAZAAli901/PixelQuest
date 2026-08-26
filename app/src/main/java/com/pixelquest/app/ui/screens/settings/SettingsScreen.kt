package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToDifficulty: () -> Unit = {},
    onNavigateToAvatar: () -> Unit = {},
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
                color = com.pixelquest.app.ui.theme.PixelGold
            )
            PixelButton(
                text = "🛡️ CHANGE DIFFICULTY",
                onClick = onNavigateToDifficulty,
                variant = PixelButtonVariant.YELLOW,
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
        },
        dataSection = {
            PixelButton(
                text = "📤 EXPORT QUEST DATA (JSON)",
                onClick = { exportLauncher.launch("pixelquest_backup.json") },
                variant = PixelButtonVariant.GREEN,
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
                variant = PixelButtonVariant.RED,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )

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
