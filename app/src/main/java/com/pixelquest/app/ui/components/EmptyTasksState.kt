package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun EmptyTasksState(
    onCreateQuestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 24.dp,
        modifier = modifier.fillMaxWidth(0.9f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚔️",
                style = PixelTypography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NO QUESTS YET",
                style = PixelTypography.displaySmall,
                color = PixelGold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your quest log is empty, brave adventurer! Create your first quest to begin your journey.",
                style = PixelTypography.bodyMedium,
                color = PixelTextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            PixelButton(
                text = "CREATE FIRST QUEST",
                onClick = onCreateQuestClick,
                variant = PixelButtonVariant.YELLOW
            )
        }
    }
}
