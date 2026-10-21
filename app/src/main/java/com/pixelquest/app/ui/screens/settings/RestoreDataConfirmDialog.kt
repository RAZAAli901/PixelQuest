package com.pixelquest.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.components.PqConfirmDialog

/**
 * Confirms restoring a backup. Uses the app's themed confirm dialog, so it matches Pixel, Light and
 * Comic instead of showing a plain Material dialog.
 */
@Composable
fun RestoreDataConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    /** What the file holds (BackupSummary), so the player sees what they're restoring. */
    summary: String? = null,
    /**
     * False on the welcome screen, where there's no hero yet: warning that the restore overwrites
     * "your current hero" there (as it did on the emulator) only made a new install look risky.
     */
    replacesAGame: Boolean = true
) {
    PqConfirmDialog(
        title = if (replacesAGame) "OVERWRITE QUEST DATA?" else "RESTORE THIS BACKUP?",
        message = (summary?.let { "This backup has $it\n\n" } ?: "") +
            if (replacesAGame) {
                "Restoring it will completely overwrite your current hero profile, streak, and tasks. Are you sure you want to proceed?"
            } else {
                "Your hero, quests and history come back from it, and the welcome steps are skipped."
            },
        confirm = "RESTORE BACKUP",
        dismiss = "CANCEL",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
