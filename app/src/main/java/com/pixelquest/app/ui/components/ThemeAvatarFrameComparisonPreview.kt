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
 * Step 19: Compose Preview comparing avatar frames across all three themes
 * (Pixel, Light, Comic) and all three tiers (Bronze, Silver, Gold) = 9 combinations.
 */
@Composable
private fun AvatarTierRow(themeTitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = themeTitle,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bronze Tier (Level 1)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PixelAvatarFrame(
                    avatarId = "avatar_1",
                    level = 1,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bronze (Lvl 1)",
                    style = PixelTypography.bodySmall,
                    fontSize = 10.sp,
                    color = PixelTheme.colors.onSurfaceVariant
                )
            }

            // Silver Tier (Level 6)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PixelAvatarFrame(
                    avatarId = "avatar_1",
                    level = 6,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Silver (Lvl 6)",
                    style = PixelTypography.bodySmall,
                    fontSize = 10.sp,
                    color = PixelTheme.colors.onSurfaceVariant
                )
            }

            // Gold Tier (Level 12)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PixelAvatarFrame(
                    avatarId = "avatar_1",
                    level = 12,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gold (Lvl 12)",
                    style = PixelTypography.bodySmall,
                    fontSize = 10.sp,
                    color = PixelTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Avatar Frame Comparison - 3 Themes x 3 Tiers (9 combinations)", showBackground = true, widthDp = 420)
@Composable
fun ThemeAvatarFrameComparisonPreview() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Pixel Theme (Dark 8-bit)
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            AvatarTierRow(themeTitle = "PIXEL THEME (DARK)")
        }

        // Light Theme (Daylight Emerald)
        PixelQuestTheme(themeMode = ThemeMode.Light) {
            AvatarTierRow(themeTitle = "LIGHT THEME (DAYLIGHT)")
        }

        // Comic Theme (Pop-Art Ink & Ribbon Badges)
        PixelQuestTheme(themeMode = ThemeMode.Comic) {
            AvatarTierRow(themeTitle = "COMIC THEME (POP-ART RIBBONS)")
        }
    }
}
