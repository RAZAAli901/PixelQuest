package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.ui.components.pixelClickable
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * One row per notification channel; tapping opens that channel's system settings, where
 * sound, vibration and importance are controlled on Android 8 and later.
 */
@Composable
fun NotificationChannelLinks(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = PixelTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "ALERT TYPES",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        NotificationChannels.userFacing.forEach { spec ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button }
                    .pixelClickable {
                        context.startActivity(NotificationChannels.systemSettingsIntent(context, spec))
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = spec.displayName, style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
                    Text(text = spec.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Text(text = "›", style = MaterialTheme.typography.labelLarge, color = colors.primary)
            }
        }
    }
}
