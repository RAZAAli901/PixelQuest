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
import androidx.compose.material3.MaterialTheme

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
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = terminology.emptyStateTitle,
                style = MaterialTheme.typography.titleMedium,
                // Theme roles: Pixel gold as before, Light amber, Comic black ink on the white panel.
                color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = terminology.emptyStateSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
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
