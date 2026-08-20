package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Preview(showBackground = true)
@Composable
fun PixelDailyProgressRingPreview() {
    PixelQuestTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // Partial progress below target
            PixelDailyProgressRing(progress = 0.5f, targetThreshold = 0.7f)
            Spacer(modifier = Modifier.height(16.dp))
            // Target met / Perfect Day
            PixelDailyProgressRing(progress = 0.75f, targetThreshold = 0.7f)
            Spacer(modifier = Modifier.height(16.dp))
            PixelPerfectDayBanner()
        }
    }
}
