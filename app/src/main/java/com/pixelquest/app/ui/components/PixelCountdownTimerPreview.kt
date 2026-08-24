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
import java.time.LocalTime

@Preview(name = "Pixel Countdown Timer States", showBackground = true)
@Composable
fun PixelCountdownTimerPreview() {
    PixelQuestTheme {
        val now = LocalTime.of(12, 0)
        Column(
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Normal (>15 mins left)
            PixelCountdownTimer(
                scheduledTime = LocalTime.of(14, 30),
                overrideNow = now
            )
            // Urgent (<15 mins left)
            PixelCountdownTimer(
                scheduledTime = LocalTime.of(12, 10),
                overrideNow = now
            )
            // Expired / Time's Up
            PixelCountdownTimer(
                scheduledTime = LocalTime.of(11, 45),
                overrideNow = now
            )
        }
    }
}
