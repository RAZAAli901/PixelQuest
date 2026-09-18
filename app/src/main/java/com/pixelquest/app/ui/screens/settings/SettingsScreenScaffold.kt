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
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun SettingsScreenScaffold(
    accountSection: @Composable () -> Unit = {},
    notificationsSection: @Composable () -> Unit = {},
    appearanceSection: @Composable () -> Unit = {},
    simpleModeSection: @Composable () -> Unit = {},
    dataSection: @Composable () -> Unit = {},
    dangerZoneSection: @Composable () -> Unit = {}
) {
    val colors = PixelTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚙️ SETTINGS",
            style = PixelTypography.titleLarge,
            color = colors.primary
        )

        // Account Section Card
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "👤 ACCOUNT", style = PixelTypography.titleMedium, color = colors.primary)
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
                Text(text = "🔔 NOTIFICATIONS", style = PixelTypography.titleMedium, color = colors.primary)
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
                Text(text = "📺 APPEARANCE & AUDIO", style = PixelTypography.titleMedium, color = colors.primary)
                appearanceSection()
            }
        }

        // Simple Mode Section Card
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "📋 SIMPLE MODE", style = PixelTypography.titleMedium, color = colors.primary)
                simpleModeSection()
            }
        }

        // Data Section Card
        PixelCard(
            variant = PixelPanelVariant.BEIGE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "💾 DATA BACKUP", style = PixelTypography.titleMedium, color = colors.primary)
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
                Text(text = "⚠️ DANGER ZONE", style = PixelTypography.titleMedium, color = colors.error)
                dangerZoneSection()
            }
        }
    }
}
