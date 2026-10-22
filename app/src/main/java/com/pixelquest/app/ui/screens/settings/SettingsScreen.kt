package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqDangerButton
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** Settings > Look's Comic / Pixel switch. */
const val SETTINGS_LOOK_TAG = "settings_look"

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToDifficulty: () -> Unit = {},
    onNavigateToAvatar: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    onResetComplete: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val askForNotifications = com.pixelquest.app.ui.notifications.rememberAskForNotifications()

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.exportBackupToUri(context, it) }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.onImportFileSelected(context, it) }
    }

    SettingsScreenScaffold(
        onNavigateBack = onNavigateBack,
        accountSection = {
            val savedName = state.profile?.username ?: "PixelHero"
            val nameDraft by viewModel.nameDraft.collectAsState()
            val shownName = nameDraft ?: savedName
            val nameError = nameDraft?.let { com.pixelquest.app.domain.HeroName.error(it) }
            PixelTextField(
                value = shownName,
                onValueChange = { viewModel.onNameDraftChanged(it) },
                label = "Hero name",
                placeholder = "Enter username",
                errorText = nameError,
                modifier = Modifier.fillMaxWidth(),
            )
            if (nameDraft != null && nameDraft != savedName) {
                PqButton("Save name", { viewModel.saveName() }, Modifier.fillMaxWidth(), enabled = nameError == null)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.pixelquest.app.ui.components.PixelAvatar(
                    com.pixelquest.app.ui.components.AvatarClass.from(state.profile?.avatarId),
                    56.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text(text = "Avatar", style = PqType.BodyStrong)
                    Text(text = "Level ${state.profile?.level ?: 1}", style = PqType.Meta)
                }
                PqButton("Change", onNavigateToAvatar, style = PqButtonStyle.Secondary, height = 38.dp)
            }
            val diffLevel = state.difficulty?.difficultyLevel ?: com.pixelquest.app.domain.model.DifficultyLevel.MEDIUM
            SettingLink(
                Icons.Rounded.Tune,
                "Difficulty",
                onNavigateToDifficulty,
                value = com.pixelquest.app.domain.DifficultyMode.getDisplayName(diffLevel),
                enabled = !state.isSimpleModeEnabled,
            )
            if (state.isSimpleModeEnabled) SettingNote(DIFFICULTY_LOCKED_IN_SIMPLE_MODE)
            SettingLink(Icons.Rounded.Cloud, "Cloud & leaderboard", onNavigateToAccount)
        },
        notificationsSection = {
            SettingSwitch(
                "Notifications",
                "Reminders and nudges from PixelQuest",
                state.isNotificationsEnabled,
                { on ->
                    if (on) askForNotifications()
                    viewModel.toggleNotifications(on)
                },
            )
            if (state.isNotificationsEnabled) {
                ExactAlarmPrompt(
                    canScheduleExact = { viewModel.canScheduleExactAlarms() },
                    onAllow = { context.startActivity(viewModel.exactAlarmSettingsIntent()) },
                )
                val reminderSoundOn by viewModel.isReminderSoundEnabled.collectAsState()
                SettingSwitch("Reminder sound", "Off, reminders arrive silently", reminderSoundOn, {
                    viewModel.setReminderSoundEnabled(it)
                })
            }
            SettingLink(Icons.Rounded.Settings, "Android's notification settings", {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            })
            NotificationChannelLinks()
        },
        appearanceSection = {
            // Settings > Look: UI v2's two skins (docs/ui-v2.md). The choice is kept and re-skins every screen.
            com.pixelquest.app.ui.components.PqFieldLabel(
                androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.pq_settings_look),
            )
            com.pixelquest.app.ui.components.PqSegmented(
                options = listOf(
                    androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.pq_look_comic),
                    androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.pq_look_pixel),
                ),
                selected = if (state.themeMode.skin == com.pixelquest.app.ui.theme.PqSkin.Pixel) 1 else 0,
                onSelect = {
                    viewModel.setThemeMode(
                        if (it ==
                            1
                        ) {
                            com.pixelquest.app.ui.theme.ThemeMode.Pixel
                        } else {
                            com.pixelquest.app.ui.theme.ThemeMode.Comic
                        },
                    )
                },
                modifier = Modifier.testTag(SETTINGS_LOOK_TAG),
            )
            SettingSwitch("Sound effects", null, state.isSoundEnabled, { viewModel.toggleSound(it) })
            SettingSwitch("Haptics", null, state.isHapticsEnabled, { viewModel.toggleHaptics(it) })
            // The scanlines are the Pixel look's own.
            if (state.themeMode.skin == com.pixelquest.app.ui.theme.PqSkin.Pixel) {
                SettingSwitch("CRT filter", "Old-screen scanlines over the Pixel look", state.isCrtEnabled, { viewModel.toggleCrt(it) })
            }
            val reduceMotionOn by viewModel.isReduceMotionEnabled.collectAsState()
            SettingSwitch("Reduce motion", "Pulses, spinners and bounces hold still", reduceMotionOn, {
                viewModel.setReduceMotionEnabled(it)
            })
            if (!reduceMotionOn && com.pixelquest.app.ui.theme.rememberReduceMotion(appSetting = false)) {
                SettingNote("Android's \"Remove animations\" is on, so motion is reduced anyway.")
            }
        },
        aiSection = {
            val signedIn by viewModel.isSignedIn.collectAsState()
            SettingNote(
                "Personalized coaching debriefs and actionable habit suggestions generated by Google Gemini. Anonymized streak trends with zero PII.",
            )
            val aiSetting = com.pixelquest.app.domain.ai.AiCoachSetting.of(
                state.isAiInsightsEnabled,
                com.pixelquest.app.domain.ai.AiAvailability.inThisBuild,
                signedIn,
            )
            if (com.pixelquest.app.domain.ai.AiAvailability.inThisBuild && !signedIn) {
                SettingNote(com.pixelquest.app.domain.ai.AiErrorCopy.SIGN_IN_REQUIRED)
                PqButton("Sign in to use AI coach", onNavigateToAccount, Modifier.fillMaxWidth())
            } else if (com.pixelquest.app.domain.ai.AiAvailability.inThisBuild) {
                // On asks first, with the privacy disclosure.
                SettingSwitch(
                    "AI coach",
                    if (aiSetting ==
                        com.pixelquest.app.domain.ai.AiCoachSetting.ENABLED
                    ) {
                        "Daily debriefs are on"
                    } else {
                        "Off until you opt in"
                    },
                    state.isAiInsightsEnabled,
                    { on -> if (on) viewModel.requestEnableAiInsights() else viewModel.disableAiInsights() },
                )
                if (state.isAiInsightsEnabled) {
                    SettingLink(Icons.Rounded.Policy, "Privacy disclosure", { viewModel.requestEnableAiInsights() })
                    val aiMessagesOn by viewModel.isAiReminderMessagesEnabled.collectAsState()
                    SettingSwitch(
                        "AI reminder messages",
                        "Once a day, Gemini writes a few short encouraging lines for your reminders from the same anonymous stats. Task names are never sent.",
                        aiMessagesOn,
                        { on ->
                            viewModel.setAiReminderMessagesEnabled(on)
                            if (on) com.pixelquest.app.worker.EncouragementPackWorker.runNow(context)
                        },
                    )
                }
            } else {
                SettingNote(
                    "This build of PixelQuest has no AI service or sign-in set up, so AI Coach can't be turned on. Everything else works as usual.",
                )
            }
        },
        islamicSection = { com.pixelquest.app.islamic.ui.IslamicSettingsSection() },
        chessSection = { com.pixelquest.app.chess.ui.ChessSettingsRoute() },
        simpleModeSection = {
            if (state.showSimpleModeHighlight && !state.isSimpleModeEnabled) {
                // A one-time tip, until dismissed.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .comic(bg = PqColors.Sky, radius = PqDimens.RadiusSm, border = PqDimens.BorderThin, shadow = 0.dp)
                        .padding(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "New: try Simple Mode", style = PqType.BodyStrong)
                        Text(
                            text = "Looking for a calm, distraction-free checklist? Switch off gamification anytime.",
                            style = PqType.Meta.copy(color = PqColors.Ink),
                        )
                    }
                    com.pixelquest.app.ui.components.DismissButton(
                        contentDescription = "Dismiss the Simple Mode tip",
                        onClick = { viewModel.dismissSimpleModeHighlight() },
                        color = PqColors.Ink,
                    )
                }
            }
            SettingSwitch(
                "Simple Mode",
                if (state.isSimpleModeEnabled) {
                    "A calm checklist: no XP, streaks, levels or celebrations. Your progress is kept, and returns when you switch it off."
                } else {
                    "Minimalist, un-gamified task tracking. Hides XP, streaks, levels, and celebration modals while preserving all background progress."
                },
                state.isSimpleModeEnabled,
                { on -> if (on) viewModel.requestEnableSimpleMode() else viewModel.toggleSimpleMode(false) },
            )
        },
        dataSection = {
            PqButton(
                "Export quest data",
                // Dated, so several backups can sit side by side.
                { exportLauncher.launch("pixelquest-backup-${java.time.LocalDate.now()}.json") },
                Modifier.fillMaxWidth(),
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.Upload,
            )
            val lastBackup by viewModel.lastBackupText.collectAsState()
            if (lastBackup.isNotEmpty()) SettingNote(lastBackup)
            val autoBackupDays by viewModel.autoBackupDays.collectAsState()
            if (autoBackupDays.isNotEmpty()) {
                // Kept on the phone by AutoBackupWorker, in case of a corrupt database or a mistake.
                SettingNote("Automatic backups on this phone (the last ${com.pixelquest.app.data.backup.AutoBackups.KEEP} days):")
                autoBackupDays.forEach { day ->
                    SettingLink(
                        Icons.Rounded.Restore,
                        "Restore " + day.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH)),
                        { viewModel.onAutoBackupSelected(day) },
                    )
                }
            }
            val canUndoRestore by viewModel.canUndoRestore.collectAsState()
            if (canUndoRestore) {
                // What the phone had before the last restore is kept until it's used or replaced.
                SettingLink(Icons.AutoMirrored.Rounded.Undo, "Undo last restore", { viewModel.undoLastRestore() })
            }
            PqButton(
                "Import quest data",
                { importLauncher.launch(arrayOf("application/json", "*/*")) },
                Modifier.fillMaxWidth(),
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.Download,
            )
            val backupMessage by viewModel.backupMessage.collectAsState()
            backupMessage?.let { SettingNote(it) }
        },
        dangerZoneSection = {
            SettingNote("Wipes every quest, your XP, level and streaks on this phone. It asks twice first.")
            PqDangerButton("Reset all progress", { viewModel.onResetProgressClicked() }, icon = Icons.Rounded.DeleteForever)
        },
    )

    val showSimpleModeDialog by viewModel.showSimpleModeDialog.collectAsState()
    if (showSimpleModeDialog) {
        SimpleModeEnableDialog(
            onConfirm = { viewModel.confirmEnableSimpleMode() },
            onDismiss = { viewModel.dismissSimpleModeDialog() },
        )
    }

    val showRestoreDialog by viewModel.showRestoreConfirmDialog.collectAsState()
    if (showRestoreDialog) {
        val restoreSummary by viewModel.pendingImportSummary.collectAsState()
        RestoreDataConfirmDialog(
            onConfirm = { viewModel.confirmImport() },
            onDismiss = { viewModel.dismissImportDialog() },
            summary = restoreSummary,
        )
    }

    val resetStepState by viewModel.resetStep.collectAsState()
    if (resetStepState > 0) {
        ResetProgressDialogSequence(
            step = resetStepState,
            onNextStep = { viewModel.advanceResetStep() },
            onConfirmWipe = { viewModel.performFullReset(onResetComplete) },
            onDismiss = { viewModel.cancelReset() },
        )
    }

    val showAiPrivacyDialog by viewModel.showAiInsightsPrivacyDialog.collectAsState()
    if (showAiPrivacyDialog) {
        AiInsightsPrivacyConfirmDialog(
            onConfirm = { viewModel.confirmEnableAiInsights() },
            onDismiss = { viewModel.dismissAiInsightsPrivacyDialog() },
        )
    }
}
