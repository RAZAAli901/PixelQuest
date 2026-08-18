package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 24.dp,
        modifier = modifier.fillMaxWidth(0.9f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚠️",
                style = PixelTypography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "QUEST LOG ERROR",
                style = PixelTypography.displaySmall,
                color = PixelRed,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = PixelTypography.bodyMedium,
                color = PixelTextWhite,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            PixelButton(
                text = "RETRY",
                onClick = onRetry,
                variant = PixelButtonVariant.YELLOW
            )
        }
    }
}
