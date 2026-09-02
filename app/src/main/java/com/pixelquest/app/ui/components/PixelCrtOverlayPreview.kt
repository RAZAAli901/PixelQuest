package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun SampleScreenContent(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = PixelTypography.titleLarge,
            color = PixelGold
        )
        Spacer(modifier = Modifier.height(16.dp))
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "RETRO QUEST LOG",
                    style = PixelTypography.titleMedium,
                    color = PixelGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete 5 quests to gain 100 XP!",
                    style = PixelTypography.bodyMedium,
                    color = PixelTextWhite
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        PixelButton(
            text = "START QUEST",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "CRT Filter OFF", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PixelCrtOverlayPreviewOff() {
    PixelQuestTheme {
        PixelCrtOverlay(enabled = false) {
            SampleScreenContent("CRT FILTER OFF")
        }
    }
}

@Preview(name = "CRT Filter ON", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PixelCrtOverlayPreviewOn() {
    PixelQuestTheme {
        PixelCrtOverlay(enabled = true) {
            SampleScreenContent("CRT FILTER ON")
        }
    }
}
