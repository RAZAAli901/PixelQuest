package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens

/**
 * Step 16: ComicDialog implementation matching PixelDialog's role, built on ComicPanel.
 * Features Bangers headline font, crisp ink border, tactile drop-shadow, and comic action buttons.
 */
@Composable
fun ComicDialog(
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
        ComicPanel(
            modifier = modifier.fillMaxWidth(0.92f),
            variant = ComicPanelVariant.SURFACE,
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title.uppercase(),
                    fontFamily = BangersFontFamily,
                    fontSize = 22.sp,
                    letterSpacing = 0.8.sp,
                    color = ComicTokens.SolidBlack
                )
                Spacer(modifier = Modifier.height(16.dp))

                CompositionLocalProvider(
                    LocalContentColor provides ComicTokens.SolidBlack
                ) {
                    content()
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDismiss != null && dismissButtonText != null) {
                        ComicButton(
                            text = dismissButtonText,
                            onClick = onDismiss,
                            variant = ComicButtonVariant.SKY_BLUE
                        )
                    }
                    if (onDismiss != null && dismissButtonText != null && onConfirm != null && confirmButtonText != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    if (onConfirm != null && confirmButtonText != null) {
                        ComicButton(
                            text = confirmButtonText,
                            onClick = onConfirm,
                            variant = ComicButtonVariant.PRIMARY
                        )
                    }
                }
            }
        }
    }
}
