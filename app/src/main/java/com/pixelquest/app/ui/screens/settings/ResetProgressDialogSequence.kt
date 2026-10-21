package com.pixelquest.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.components.PqConfirmDialog

/**
 * Two-step confirmation before wiping all progress. Both steps use the app's themed confirm
 * dialog, which also plays the warning haptic on confirm.
 */
@Composable
fun ResetProgressDialogSequence(
    step: Int,
    onNextStep: () -> Unit,
    onConfirmWipe: () -> Unit,
    onDismiss: () -> Unit
) {
    when (step) {
        1 -> PqConfirmDialog(
            title = "RESET ALL PROGRESS?",
            message = "This will delete all your quests and their history, your streak, XP and level, and restart setup. If you're on the leaderboard, your numbers there start over too, under the same name. Your theme, sound and notification settings are kept. Proceed?",
            confirm = "YES, CONTINUE",
            dismiss = "CANCEL",
            onConfirm = onNextStep,
            onDismiss = onDismiss
        )
        2 -> PqConfirmDialog(
            title = "FINAL WARNING",
            message = "This action CANNOT be undone. All data will be permanently wiped and reset to day one.",
            confirm = "CONFIRM WIPEOUT",
            dismiss = "CANCEL",
            onConfirm = onConfirmWipe,
            onDismiss = onDismiss
        )
    }
}
