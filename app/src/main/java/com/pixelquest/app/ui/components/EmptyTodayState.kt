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
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun EmptyTodayState(
    onCreateQuestClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false
) {
    val terminology = com.pixelquest.app.domain.model.TaskTerminology.forMode(isSimpleMode)
    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors
    PixelCard(
        variant = PixelPanelVariant.BORDER,
        contentPadding = 24.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isSimpleMode) "📋" else "🏰",
                style = PixelTypography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = terminology.emptyStateTitle,
                style = PixelTypography.titleMedium,
                color = if (isSimpleMode) colors.primary else PixelGold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = terminology.emptyStateSubtitle,
                style = PixelTypography.bodyMedium,
                color = PixelTextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            PixelButton(
                text = terminology.createButtonText,
                onClick = onCreateQuestClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}
