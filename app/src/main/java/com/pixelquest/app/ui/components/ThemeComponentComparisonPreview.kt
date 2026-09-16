package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 10: Compose Previews comparing dark (Pixel) vs light component rendering side-by-side.
 */
@Composable
private fun ComponentShowcase(label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        // Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PixelButton(
                text = "QUEST",
                onClick = {},
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.weight(1f)
            )
            PixelButton(
                text = "ACTION",
                onClick = {},
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.weight(1f)
            )
        }

        // Cards
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BORDER,
            contentPadding = 12.dp
        ) {
            Text(
                text = "Standard Card Panel",
                style = PixelTypography.bodyMedium,
                color = PixelTheme.colors.onSurface,
                fontSize = 10.sp
            )
        }

        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BEIGE,
            contentPadding = 12.dp
        ) {
            Text(
                text = "Highlight / Beige Panel",
                style = PixelTypography.bodyMedium,
                color = PixelTheme.colors.onSurface,
                fontSize = 10.sp
            )
        }

        // Avatar Frames
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAvatarFrame(
                avatarId = "avatar_hero",
                level = 3,
                size = 48.dp
            )
            PixelAvatarFrame(
                avatarId = "avatar_mage",
                level = 7,
                size = 48.dp
            )
            PixelAvatarFrame(
                avatarId = "avatar_paladin",
                level = 15,
                size = 48.dp
            )
        }
    }
}

@Preview(name = "Side-by-Side Theme Comparison", widthDp = 760, heightDp = 620)
@Composable
fun ThemeComponentComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Pixel Mode (Dark)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                ComponentShowcase(label = "PIXEL MODE (DARK)")
            }
        }

        // Light Mode
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                ComponentShowcase(label = "LIGHT MODE (DAYLIGHT)")
            }
        }
    }
}
