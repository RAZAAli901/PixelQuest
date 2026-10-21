package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * One row per notification channel; tapping opens that channel's system settings, where
 * sound, vibration and importance are controlled on Android 8 and later.
 */
@Composable
fun NotificationChannelLinks(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PqFieldLabel("Alert types")
        NotificationChannels.userFacing.forEach { spec ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { context.startActivity(NotificationChannels.systemSettingsIntent(context, spec)) }
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = spec.displayName, style = PqType.BodyStrong)
                    Text(text = spec.description, style = PqType.Meta)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = PqColors.Muted)
            }
        }
    }
}
