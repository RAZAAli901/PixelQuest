package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun SettingsScreenScaffold(
    accountSection: @Composable () -> Unit = {},
    notificationsSection: @Composable () -> Unit = {},
    appearanceSection: @Composable () -> Unit = {},
    dataSection: @Composable () -> Unit = {},
    dangerZoneSection: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚙️ SETTINGS",
            style = PixelTypography.titleLarge,
            color = PixelGold
        )

        // Account Section Card
        PixelCard(
            variant = PixelPanelVariant.YELLOW,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "👤 ACCOUNT", style = PixelTypography.titleMedium, color = PixelGold)
                accountSection()
            }
        }

        // Notifications Section Card
        PixelCard(
            variant = PixelPanelVariant.BLUE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "🔔 NOTIFICATIONS", style = PixelTypography.titleMedium, color = PixelGold)
                notificationsSection()
            }
        }

        // Appearance Section Card
        PixelCard(
            variant = PixelPanelVariant.BLUE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "📺 APPEARANCE & AUDIO", style = PixelTypography.titleMedium, color = PixelGold)
                appearanceSection()
            }
        }

        // Data Section Card
        PixelCard(
            variant = PixelPanelVariant.BEIGE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "💾 DATA BACKUP", style = PixelTypography.titleMedium, color = PixelGold)
                dataSection()
            }
        }

        // Danger Zone Section Card
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "⚠️ DANGER ZONE", style = PixelTypography.titleMedium, color = PixelRed)
                dangerZoneSection()
            }
        }
    }
}
