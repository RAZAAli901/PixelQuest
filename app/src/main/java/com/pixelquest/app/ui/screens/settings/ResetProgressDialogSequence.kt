package com.pixelquest.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.components.PixelConfirmDialog

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
        1 -> PixelConfirmDialog(
            title = "⚠️ RESET ALL PROGRESS?",
            message = "This will delete all your quests, level progress, streak history, and settings. Proceed?",
            confirmText = "YES, CONTINUE",
            dismissText = "CANCEL",
            onConfirm = onNextStep,
            onDismiss = onDismiss
        )
        2 -> PixelConfirmDialog(
            title = "🔥 FINAL WARNING",
            message = "This action CANNOT be undone. All data will be permanently wiped and reset to day one.",
            confirmText = "CONFIRM WIPEOUT",
            dismissText = "CANCEL",
            onConfirm = onConfirmWipe,
            onDismiss = onDismiss
        )
    }
}
