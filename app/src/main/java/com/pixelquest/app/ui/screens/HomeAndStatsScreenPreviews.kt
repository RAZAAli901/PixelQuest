package com.pixelquest.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.Pixel7DayHistoryStrip
import com.pixelquest.app.ui.theme.PixelQuestTheme
import java.time.LocalDate

@Preview(showBackground = true)
@Composable
fun Pixel7DayHistoryStripPreview() {
    PixelQuestTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            val sampleDays = (6 downTo 0).map { daysAgo ->
                Pair(LocalDate.now().minusDays(daysAgo.toLong()), daysAgo % 2 == 0)
            }
            Pixel7DayHistoryStrip(days = sampleDays)
        }
    }
}
