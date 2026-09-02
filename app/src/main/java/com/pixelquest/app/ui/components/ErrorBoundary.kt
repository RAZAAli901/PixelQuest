package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun ErrorBoundary(
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var hasError by remember { mutableStateOf(false) }

    if (hasError || errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PixelCard(
                variant = PixelPanelVariant.BORDER,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 20.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚠️ SOMETHING WENT WRONG",
                        style = PixelTypography.titleMedium,
                        color = PixelRed,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = errorMessage ?: "An unexpected error occurred while rendering this screen.",
                        style = PixelTypography.bodySmall,
                        color = PixelTextWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (onRetry != null) {
                        PixelButton(
                            text = "🔄 TRY AGAIN",
                            onClick = {
                                hasError = false
                                onRetry()
                            },
                            variant = PixelButtonVariant.YELLOW
                        )
                    }
                }
            }
        }
    } else {
        content()
    }
}
