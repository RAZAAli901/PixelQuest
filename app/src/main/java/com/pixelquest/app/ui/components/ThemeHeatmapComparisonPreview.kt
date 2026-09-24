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
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 30: Compose Preview comparing Heatmap cell and grid rendering
 * across Pixel, Light, and Comic themes.
 */
@Composable
private fun HeatmapThemeSection(themeTitle: String) {
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

        // Status legend cells
        val statuses = listOf(
            "Empty" to DailyStatus.NO_TASKS_SCHEDULED,
            "Partial" to DailyStatus.PARTIAL,
            "Perfect" to DailyStatus.PERFECT,
            "Missed" to DailyStatus.MISSED
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            statuses.forEach { (label, status) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelHeatmapCell(
                        status = status,
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = PixelTypography.labelSmall,
                        fontSize = 9.sp,
                        color = PixelTheme.colors.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sample 14-day Activity Mini-Grid
        Text(
            text = "SAMPLE 14-DAY ACTIVITY STRETCH",
            style = PixelTypography.bodySmall,
            color = PixelTheme.colors.onSurfaceVariant,
            fontSize = 9.sp
        )
        val sampleGrid = listOf(
            DailyStatus.PERFECT, DailyStatus.PERFECT, DailyStatus.PARTIAL, DailyStatus.PERFECT,
            DailyStatus.MISSED, DailyStatus.PERFECT, DailyStatus.PERFECT, DailyStatus.PARTIAL,
            DailyStatus.PERFECT, DailyStatus.PERFECT, DailyStatus.PERFECT, DailyStatus.NO_TASKS_SCHEDULED,
            DailyStatus.PERFECT, DailyStatus.PERFECT
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sampleGrid.forEach { status ->
                PixelHeatmapCell(
                    status = status,
                    size = 16.dp
                )
            }
        }
    }
}

@Preview(name = "Heatmap Comparison - Pixel vs Light vs Comic", showBackground = true, widthDp = 420)
@Composable
fun ThemeHeatmapComparisonPreview() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Pixel Theme (Dark 8-bit)
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            HeatmapThemeSection(themeTitle = "PIXEL THEME (DARK 8-BIT)")
        }

        // Light Theme (Daylight Ivory/Emerald)
        PixelQuestTheme(themeMode = ThemeMode.Light) {
            HeatmapThemeSection(themeTitle = "LIGHT THEME (DAYLIGHT)")
        }

        // Comic Theme (Pop-Art Ink & Container Colors)
        PixelQuestTheme(themeMode = ThemeMode.Comic) {
            HeatmapThemeSection(themeTitle = "COMIC THEME (POP-ART INK)")
        }
    }
}
