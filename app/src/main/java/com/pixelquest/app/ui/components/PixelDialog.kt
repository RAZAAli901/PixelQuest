package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * A dialog, as the screens from before UI v2 ask for one: now the skin's own card (docs/ui-v2.md),
 * with its title, [content], and its buttons side by side, the quieter one first. Text is as given.
 */
@Composable
fun PixelDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmButtonText: String? = "YES",
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String? = "NO",
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        PqCard(modifier.fillMaxWidth(), shadow = PqDimens.ShadowLg, radius = 18.dp) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = title, style = PqType.CardTitle)
                CompositionLocalProvider(LocalContentColor provides PqColors.Ink2) { content() }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onDismiss != null && dismissButtonText != null) {
                        LegacyButton(dismissButtonText, onDismiss, Modifier.weight(1f), fill = PqColors.Paper, textColor = PqColors.Ink, enabled = true)
                    }
                    if (onConfirm != null && confirmButtonText != null) {
                        LegacyButton(confirmButtonText, onConfirm, Modifier.weight(1f), fill = PqColors.Red, textColor = PqColors.OnRed, enabled = true)
                    }
                }
            }
        }
    }
}
