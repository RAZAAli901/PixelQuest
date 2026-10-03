package com.pixelquest.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.components.PixelConfirmDialog

/**
 * Confirms restoring a backup. Uses the app's themed confirm dialog, so it matches Pixel, Light and
 * Comic instead of showing a plain Material dialog.
 */
@Composable
fun RestoreDataConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PixelConfirmDialog(
        title = "💾 OVERWRITE QUEST DATA?",
        message = "Restoring this backup file will completely overwrite your current hero profile, streak, and tasks. Are you sure you want to proceed?",
        confirmText = "RESTORE BACKUP",
        dismissText = "CANCEL",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
