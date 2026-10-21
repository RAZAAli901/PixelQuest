package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PqDialog
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * Asked before Simple Mode is turned on: what it hides, that nothing stops being recorded, and that
 * it can be turned off again with everything kept.
 */
@Composable
fun SimpleModeEnableDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    PqDialog(
        title = "Simple Mode",
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirm = "Turn on",
        onConfirm = {
            PixelHaptics.performSuccessPattern(haptic)
            onConfirm()
        },
        dismiss = "Cancel",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PixelQuest becomes a clean, calm task checklist, without the game.",
                style = PqType.Body
            )
            ExplainingRow(Icons.Rounded.VisibilityOff, "Hidden", "Streaks, XP, levels and celebrations aren't shown.")
            ExplainingRow(Icons.Rounded.Shield, "Still recorded", "Your habits and streaks keep being recorded in the background.")
            ExplainingRow(Icons.Rounded.Restore, "Reversible", "Turn it off whenever you like, with all your progress there.")
        }
    }
}

@Composable
private fun ExplainingRow(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = PqType.BodyStrong)
            Text(text = description, style = PqType.Body.copy(color = PqColors.Ink2))
        }
    }
}
