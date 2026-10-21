package com.pixelquest.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/** A yes-or-no question about something that can't be undone: a [PixelDialog] with [message]. */
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
    val haptic = LocalHapticFeedback.current
    PixelDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmButtonText = confirmText,
        onConfirm = {
            PixelHaptics.performWarning(haptic)
            onConfirm()
        },
        dismissButtonText = dismissText,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Text(text = message, style = PqType.Body.copy(color = PqColors.Ink2))
    }
}
