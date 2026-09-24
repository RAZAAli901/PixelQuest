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
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 26: Compose Preview comparing Category and Difficulty icon treatment
 * across Pixel, Light, and Comic themes.
 */
@Composable
private fun IconShowcaseSection(themeTitle: String) {
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

        // Categories Row
        Text(
            text = "CATEGORY ICONS",
            style = PixelTypography.bodySmall,
            color = PixelTheme.colors.onSurfaceVariant,
            fontSize = 10.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TaskCategory.values().forEach { category ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelCategoryIcon(
                        category = category,
                        size = 28.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.displayName,
                        style = PixelTypography.labelSmall,
                        fontSize = 9.sp,
                        color = PixelTheme.colors.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Difficulty Icons Row
        Text(
            text = "DIFFICULTY TIER ICONS",
            style = PixelTypography.bodySmall,
            color = PixelTheme.colors.onSurfaceVariant,
            fontSize = 10.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DifficultyLevel.values().forEach { level ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelDifficultyIcon(
                        level = level,
                        size = 28.dp,
                        isSelected = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = level.name,
                        style = PixelTypography.labelSmall,
                        fontSize = 9.sp,
                        color = PixelTheme.colors.onSurface
                    )
                }
            }
        }
    }
}

@Preview(name = "Icon Treatment Comparison - Pixel vs Light vs Comic", showBackground = true, widthDp = 420)
@Composable
fun ThemeIconComparisonPreview() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Pixel Theme (Retro Arcade Dark)
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            IconShowcaseSection(themeTitle = "PIXEL THEME (DARK ARCADE)")
        }

        // Light Theme (Daylight Amber/Emerald)
        PixelQuestTheme(themeMode = ThemeMode.Light) {
            IconShowcaseSection(themeTitle = "LIGHT THEME (DAYLIGHT)")
        }

        // Comic Theme (Pop-Art Badges)
        PixelQuestTheme(themeMode = ThemeMode.Comic) {
            IconShowcaseSection(themeTitle = "COMIC THEME (POP-ART BADGES)")
        }
    }
}
