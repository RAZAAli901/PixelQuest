package com.pixelquest.app.ui.screens.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun RestoreDataConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PixelSurfaceDark,
        title = {
            Text(
                text = "💾 OVERWRITE QUEST DATA?",
                style = PixelTypography.titleMedium,
                color = PixelGold
            )
        },
        text = {
            Text(
                text = "Restoring this backup file will completely overwrite your current hero profile, streak, and tasks. Are you sure you want to proceed?",
                style = PixelTypography.bodyMedium,
                color = PixelTextWhite
            )
        },
        confirmButton = {
            PixelButton(
                text = "RESTORE BACKUP",
                onClick = onConfirm,
                variant = PixelButtonVariant.YELLOW
            )
        },
        dismissButton = {
            PixelButton(
                text = "CANCEL",
                onClick = onDismiss,
                variant = PixelButtonVariant.BLUE
            )
        }
    )
}
