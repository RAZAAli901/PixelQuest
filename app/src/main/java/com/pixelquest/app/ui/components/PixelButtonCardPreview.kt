package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Composable
fun PixelButtonCardPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BORDER
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "QUEST BOARD",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Complete daily quests to gain XP!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                PixelButton(
                    text = "START QUEST",
                    onClick = {},
                    variant = PixelButtonVariant.YELLOW
                )
                Spacer(modifier = Modifier.height(8.dp))
                PixelButton(
                    text = "VIEW STATS",
                    onClick = {},
                    variant = PixelButtonVariant.BLUE
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12121E)
@Composable
fun PixelButtonCardPreview() {
    PixelQuestTheme {
        PixelButtonCardPreviewContent()
    }
}
