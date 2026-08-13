package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
        PixelCard(
            modifier = modifier.fillMaxWidth(0.92f),
            variant = PixelPanelVariant.BORDER,
            contentPadding = 20.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (onDismiss != null && dismissButtonText != null) {
                        PixelButton(
                            text = dismissButtonText,
                            onClick = onDismiss,
                            variant = PixelButtonVariant.BLUE
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    if (onConfirm != null && confirmButtonText != null) {
                        PixelButton(
                            text = confirmButtonText,
                            onClick = onConfirm,
                            variant = PixelButtonVariant.YELLOW
                        )
                    }
                }
            }
        }
    }
}
