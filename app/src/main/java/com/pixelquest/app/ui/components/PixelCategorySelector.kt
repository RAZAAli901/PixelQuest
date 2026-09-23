package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode
    if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        ComicCategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            modifier = modifier,
            label = label
        )
        return
    }

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
            TaskCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                val variant = if (isSelected) PixelPanelVariant.BLUE else PixelPanelVariant.BORDER

                PixelCard(
                    variant = variant,
                    contentPadding = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCategorySelected(category) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = category.iconResId),
                            contentDescription = category.displayName,
                            colorFilter = com.pixelquest.app.ui.theme.PixelThemeAssetFilter.forTheme(
                                com.pixelquest.app.ui.theme.PixelTheme.mode,
                                if (isSelected) com.pixelquest.app.ui.theme.PixelTheme.colors.primary else com.pixelquest.app.ui.theme.PixelTheme.colors.onSurfaceVariant
                            ),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.displayName,
                            style = PixelTypography.bodySmall,
                            color = if (isSelected) PixelCyan else PixelTextMuted
                        )
                    }
                }
            }
        }
    }
}
