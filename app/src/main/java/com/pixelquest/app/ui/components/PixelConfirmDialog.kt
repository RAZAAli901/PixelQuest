package com.pixelquest.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelConfirmDialog(
    title: String = "ABANDON QUEST?",
    message: String = "Are you sure you want to delete this quest?",
    confirmText: String = "DELETE",
    dismissText: String = "CANCEL",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmButtonText = confirmText,
        onConfirm = onConfirm,
        dismissButtonText = dismissText,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = message,
            style = PixelTypography.bodyMedium,
            color = PixelTextWhite
        )
    }
}
