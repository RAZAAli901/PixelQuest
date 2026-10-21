package com.pixelquest.app.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * Over every screen while notifications are off (once the player has said no to Android's ask):
 * reminders can't reach them, and Allow, to ask again or open the settings.
 */
@Composable
fun NotificationsOffBanner(onAllow: () -> Unit, modifier: Modifier = Modifier) {
    PqCard(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            CategoryTile(Icons.Rounded.NotificationsOff, PqColors.Rose, size = 40.dp, iconSize = 22.dp, radius = 10.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Reminders are off", style = PqType.BodyStrong)
                Text(text = "Allow notifications so quest reminders can reach you.", style = PqType.Meta)
            }
            PqButton(text = "Allow", onClick = onAllow, style = PqButtonStyle.Secondary)
        }
    }
}
