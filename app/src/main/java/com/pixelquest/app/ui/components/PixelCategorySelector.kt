package com.pixelquest.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelCategorySelector(
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "CATEGORY"
) {
    val categories = listOf(
        TaskCategory.FITNESS to "💪 Fitness",
        TaskCategory.PRODUCTIVITY to "📚 Study",
        TaskCategory.HEALTH to "❤️ Health",
        TaskCategory.MINDFULNESS to "🧘 Mind",
        TaskCategory.CUSTOM to "⭐ Custom"
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { (category, displayStr) ->
                val isSelected = selectedCategory == category
                val variant = if (isSelected) PixelPanelVariant.BLUE else PixelPanelVariant.BORDER

                PixelCard(
                    variant = variant,
                    contentPadding = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCategorySelected(category) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayStr,
                            style = PixelTypography.bodySmall,
                            color = if (isSelected) PixelCyan else PixelTextMuted
                        )
                    }
                }
            }
        }
    }
}
