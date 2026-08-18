package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelSnackbar(
    message: String,
    modifier: Modifier = Modifier
) {
    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚠️ $message",
                style = PixelTypography.bodyMedium,
                color = PixelRed,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
