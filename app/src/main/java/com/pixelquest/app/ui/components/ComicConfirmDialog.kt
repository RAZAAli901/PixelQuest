package com.pixelquest.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.ComicTokens

/**
 * Step 17: ComicConfirmDialog matching PixelConfirmDialog's role.
 * Used for delete/reset/logout confirmations with Bangers headline, ink panel shell,
 * warning haptics, and high-contrast ComicButton action pairs.
 */
@Composable
fun ComicConfirmDialog(
    title: String = "ABANDON QUEST?",
    message: String = "Are you sure you want to delete this quest?",
    confirmText: String = "DELETE",
    dismissText: String = "CANCEL",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    ComicDialog(
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
        Text(
            text = message,
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = ComicTokens.SolidBlack
        )
    }
}
