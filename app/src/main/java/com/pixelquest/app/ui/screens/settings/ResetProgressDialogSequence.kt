package com.pixelquest.app.ui.screens.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun ResetProgressDialogSequence(
    step: Int,
    onNextStep: () -> Unit,
    onConfirmWipe: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    if (step == 1) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PixelSurfaceDark,
            title = {
                Text(text = "⚠️ RESET ALL PROGRESS?", style = PixelTypography.titleMedium, color = PixelRed)
            },
            text = {
                Text(
                    text = "This will delete all your quests, level progress, streak history, and settings. Proceed?",
                    style = PixelTypography.bodyMedium,
                    color = PixelTextWhite
                )
            },
            confirmButton = {
                PixelButton(text = "YES, CONTINUE", onClick = {
                    com.pixelquest.app.ui.haptics.PixelHaptics.performWarning(haptic)
                    onNextStep()
                }, variant = PixelButtonVariant.YELLOW)
            },
            dismissButton = {
                PixelButton(text = "CANCEL", onClick = onDismiss, variant = PixelButtonVariant.BLUE)
            }
        )
    } else if (step == 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PixelSurfaceDark,
            title = {
                Text(text = "🔥 FINAL WARNING", style = PixelTypography.titleMedium, color = PixelRed)
            },
            text = {
                Text(
                    text = "This action CANNOT be undone. All data will be permanently wiped and reset to day one.",
                    style = PixelTypography.bodyMedium,
                    color = PixelTextWhite
                )
            },
            confirmButton = {
                PixelButton(text = "CONFIRM WIPEOUT", onClick = {
                    com.pixelquest.app.ui.haptics.PixelHaptics.performWarning(haptic)
                    onConfirmWipe()
                }, variant = PixelButtonVariant.YELLOW)
            },
            dismissButton = {
                PixelButton(text = "CANCEL", onClick = onDismiss, variant = PixelButtonVariant.BLUE)
            }
        )
    }
}
