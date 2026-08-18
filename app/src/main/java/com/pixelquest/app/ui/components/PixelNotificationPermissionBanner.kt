package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelNotificationPermissionBanner(
    onRequestPermission: () -> Unit,
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
                text = "⚠️",
                style = PixelTypography.titleMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REMINDERS DISABLED",
                    style = PixelTypography.labelLarge,
                    color = PixelRed
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Notification permission is required for quest alarms to fire.",
                    style = PixelTypography.bodySmall,
                    color = PixelGold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            PixelButton(
                text = "ALLOW",
                onClick = onRequestPermission,
                variant = PixelButtonVariant.YELLOW
            )
        }
    }
}
