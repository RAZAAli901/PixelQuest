package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Preview(showBackground = true)
@Composable
fun PixelBarChartPreview() {
    PixelQuestTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp)
        ) {
            val improvingTrend = listOf(
                "W-3" to 0.40f,
                "W-2" to 0.60f,
                "W-1" to 0.80f,
                "NOW" to 0.95f
            )

            val decliningTrend = listOf(
                "W-3" to 0.90f,
                "W-2" to 0.70f,
                "W-1" to 0.50f,
                "NOW" to 0.30f
            )

            val flatHighTrend = listOf(
                "W-3" to 0.85f,
                "W-2" to 0.85f,
                "W-1" to 0.90f,
                "NOW" to 0.88f
            )

            PixelBarChart(weeklyData = improvingTrend)
            PixelBarChart(weeklyData = decliningTrend)
            PixelBarChart(weeklyData = flatHighTrend)
        }
    }
}
