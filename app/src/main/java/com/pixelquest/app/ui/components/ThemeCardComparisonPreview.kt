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
import androidx.compose.ui.Alignment
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
 * Step 14: Compose Preview comparing card rendering across all three themes side-by-side:
 * Pixel (Dark Retro 9-patch), Light (Daylight crisp border), and Comic (Ink Vector & Flat Shadow).
 */
@Composable
private fun CardComparisonColumn(themeTitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = themeTitle,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        // 1. Standard Border Panel
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BORDER,
            contentPadding = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Standard Card Panel",
                    style = PixelTypography.labelLarge,
                    color = PixelTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Base container for quest lists and details.",
                    style = PixelTypography.bodyMedium,
                    color = PixelTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        // 2. Blue / Sky Blue Container
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BLUE,
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COOL CONTAINER",
                    style = PixelTypography.labelMedium,
                    color = if (PixelTheme.mode == ThemeMode.Pixel) PixelTheme.colors.onSurface else ComicTokens.SolidBlack
                )
                Text(
                    text = "ACTIVE",
                    style = PixelTypography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (PixelTheme.mode == ThemeMode.Pixel) PixelTheme.colors.secondary else ComicTokens.SolidBlack
                )
            }
        }

        // 3. Beige / Burnt Orange Container
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BEIGE,
            contentPadding = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "WARM ACTION CONTAINER",
                    style = PixelTypography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (PixelTheme.mode == ThemeMode.Pixel) PixelTheme.colors.onSurface else ComicTokens.SolidBlack
                )
                Text(
                    text = "Highlighting daily priorities and streaks.",
                    style = PixelTypography.bodyMedium,
                    fontSize = 10.sp,
                    color = if (PixelTheme.mode == ThemeMode.Pixel) PixelTheme.colors.onSurfaceVariant else ComicTokens.SolidBlack
                )
            }
        }

        // 4. Lavender Container
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.LAVENDER,
            contentPadding = 12.dp
        ) {
            Text(
                text = "LAVENDER ACCENT PANEL",
                style = PixelTypography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (PixelTheme.mode == ThemeMode.Pixel) PixelTheme.colors.onSurface else ComicTokens.SolidBlack
            )
        }
    }
}

@Preview(name = "Card 3-Theme Comparison Preview", widthDp = 1080, heightDp = 620)
@Composable
fun ThemeCardComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                CardComparisonColumn(themeTitle = "PIXEL MODE")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                CardComparisonColumn(themeTitle = "LIGHT MODE")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                CardComparisonColumn(themeTitle = "COMIC MODE")
            }
        }
    }
}
