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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode
import java.time.LocalDate

private fun createSampleHeatmapData(): Map<LocalDate, DailyStatus> {
    val today = LocalDate.now()
    val sampleData = mutableMapOf<LocalDate, DailyStatus>()
    (0..90).forEach { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val status = when (daysAgo % 5) {
            0 -> DailyStatus.PERFECT
            1 -> DailyStatus.PARTIAL
            2 -> DailyStatus.MISSED
            3 -> DailyStatus.NO_TASKS_SCHEDULED
            else -> DailyStatus.PERFECT
        }
        sampleData[date] = status
    }
    return sampleData
}

@Preview(name = "Heatmap - Pixel Mode (Dark)", showBackground = true)
@Composable
fun PixelCalendarHeatmapDarkPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        Box(
            modifier = Modifier
                .background(PixelTheme.colors.background)
                .padding(16.dp)
        ) {
            val today = LocalDate.now()
            PixelCalendarHeatmap(
                statusMap = createSampleHeatmapData(),
                startDate = today.minusMonths(3),
                endDate = today
            )
        }
    }
}

@Preview(name = "Heatmap - Light Mode (Daylight)", showBackground = true)
@Composable
fun PixelCalendarHeatmapLightPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Light) {
        Box(
            modifier = Modifier
                .background(PixelTheme.colors.background)
                .padding(16.dp)
        ) {
            val today = LocalDate.now()
            PixelCalendarHeatmap(
                statusMap = createSampleHeatmapData(),
                startDate = today.minusMonths(3),
                endDate = today
            )
        }
    }
}

/**
 * Step 25: Compose Preview comparing the heatmap rendered in both Pixel (Dark) and Light themes side-by-side.
 */
@Preview(name = "Heatmap Theme Comparison (Dark vs Light)", widthDp = 760, heightDp = 420)
@Composable
fun PixelCalendarHeatmapComparisonPreview() {
    val today = LocalDate.now()
    val sampleData = createSampleHeatmapData()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dark Theme Section
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixelTheme.colors.background)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "PIXEL THEME (RETRO DARK RAMP)",
                        style = PixelTypography.titleMedium,
                        color = PixelTheme.colors.primary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PixelCalendarHeatmap(
                        statusMap = sampleData,
                        startDate = today.minusMonths(2),
                        endDate = today
                    )
                }
            }
        }

        // Light Theme Section
        PixelQuestTheme(themeMode = ThemeMode.Light) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixelTheme.colors.background)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "LIGHT THEME (DAYLIGHT HIGH-CONTRAST RAMP)",
                        style = PixelTypography.titleMedium,
                        color = PixelTheme.colors.primary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PixelCalendarHeatmap(
                        statusMap = sampleData,
                        startDate = today.minusMonths(2),
                        endDate = today
                    )
                }
            }
        }
    }
}
