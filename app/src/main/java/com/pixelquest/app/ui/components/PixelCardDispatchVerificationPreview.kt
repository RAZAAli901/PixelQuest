package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 13: Verification preview ensuring existing PixelCard usages across the app
 * (quest cards, stat cards, highlight cards, lavender accents) render correctly
 * when dispatched in Comic mode, with proper shadow clearance and border ink.
 */
@Composable
private fun SampleCardShowcase(themeName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = themeName,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        // Quest Card (BORDER variant -> Surface panel in Comic)
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BORDER,
            contentPadding = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "QUEST: Defeat the Code Dragon",
                    style = PixelTypography.labelLarge,
                    color = PixelTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Complete 5 daily quests to earn +150 XP.",
                    style = PixelTypography.bodyMedium,
                    color = PixelTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        // Stat Card (BLUE variant -> Sky Blue panel in Comic)
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BLUE,
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STREAK COUNT",
                    style = PixelTypography.labelMedium,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = "7 DAYS",
                    style = PixelTypography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = ComicTokens.SolidBlack
                )
            }
        }

        // Highlight Card (BEIGE variant -> Burnt Orange panel in Comic)
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BEIGE,
            contentPadding = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "DAILY REWARD AVAILABLE",
                    style = PixelTypography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = "Claim your mystery chest now!",
                    style = PixelTypography.bodyMedium,
                    fontSize = 10.sp,
                    color = ComicTokens.SolidBlack
                )
            }
        }

        // Accent Card (LAVENDER variant -> Lavender panel in Comic)
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.LAVENDER,
            contentPadding = 12.dp
        ) {
            Text(
                text = "SPECIAL EVENT: DOUBLE XP WEEKEND",
                style = PixelTypography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ComicTokens.SolidBlack
            )
        }
    }
}

@Preview(name = "PixelCard Dispatch Verification", widthDp = 1080, heightDp = 640)
@Composable
fun PixelCardDispatchVerificationPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                SampleCardShowcase(themeName = "PIXEL (DARK RETRO)")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                SampleCardShowcase(themeName = "LIGHT (DAYLIGHT)")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                SampleCardShowcase(themeName = "COMIC (GATED PREVIEW)")
            }
        }
    }
}
