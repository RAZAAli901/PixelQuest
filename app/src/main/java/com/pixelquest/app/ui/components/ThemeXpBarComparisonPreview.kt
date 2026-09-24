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
 * Step 14: Compose Preview comparing XP bar rendering across all three themes:
 * Pixel (Dark Retro 8-bit), Light (Daylight Emerald), and Comic (Diagonal Energy Striped + Level Badge).
 */
@Composable
private fun XpBarShowcaseColumn(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        val xpStates = listOf(
            Triple("Level 1 (Empty)", 0, 10),
            Triple("Level 3 (Quarter)", 25, 100),
            Triple("Level 5 (Halfway)", 50, 100),
            Triple("Level 12 (Nearly Full)", 85, 100),
            Triple("Level 42 (Max / Full)", 100, 100)
        )

        xpStates.forEach { (label, current, max) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = PixelTypography.bodySmall,
                    color = PixelTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                PixelXpBar(
                    currentProgress = current,
                    maxProgress = max,
                    level = when {
                        current == 0 -> 1
                        current <= 25 -> 3
                        current <= 50 -> 5
                        current <= 85 -> 12
                        else -> 42
                    }
                )
            }
        }
    }
}

@Preview(name = "XP Bar Comparison - Pixel vs Light vs Comic", showBackground = true, widthDp = 400)
@Composable
fun ThemeXpBarComparisonPreview() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Pixel Theme (Dark)
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            XpBarShowcaseColumn(title = "PIXEL THEME (DARK)")
        }

        // Light Theme (Daylight)
        PixelQuestTheme(themeMode = ThemeMode.Light) {
            XpBarShowcaseColumn(title = "LIGHT THEME (DAYLIGHT)")
        }

        // Comic Theme (Vector Ink / Newsprint)
        PixelQuestTheme(themeMode = ThemeMode.Comic) {
            XpBarShowcaseColumn(title = "COMIC THEME (INK & ENERGY)")
        }
    }
}
