package com.pixelquest.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelRecurrenceSelector(
    selectedType: RecurrenceType,
    onTypeSelected: (RecurrenceType) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "RECURRENCE"
) {
    val options = listOf(
        RecurrenceType.DAILY to "Daily",
        RecurrenceType.WEEKLY to "Weekly",
        RecurrenceType.ONE_TIME to "One-Time"
    )

    Column(modifier = modifier) {
        Text(
            text = label,
            style = PixelTypography.labelLarge,
            color = PixelGold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (type, typeName) ->
                val isSelected = selectedType == type
                val variant = if (isSelected) PixelPanelVariant.BLUE else PixelPanelVariant.BORDER

                PixelCard(
                    variant = variant,
                    contentPadding = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTypeSelected(type) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typeName,
                            style = PixelTypography.labelSmall,
                            color = if (isSelected) PixelCyan else PixelTextMuted
                        )
                    }
                }
            }
        }
    }
}
