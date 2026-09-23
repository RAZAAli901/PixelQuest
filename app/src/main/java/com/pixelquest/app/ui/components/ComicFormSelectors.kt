package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens

/**
 * Step 25: ComicCategorySelector matching PixelCategorySelector's role.
 * Features Bangers label, ComicPanel category chips with Sky Blue selected fills,
 * crisp black borders, and high-contrast comic icons.
 */
@Composable
fun ComicCategorySelector(
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "CATEGORY"
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = BangersFontFamily,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp,
            color = ComicTokens.SolidBlack,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TaskCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                val variant = if (isSelected) ComicPanelVariant.SKY_BLUE else ComicPanelVariant.SURFACE

                ComicPanel(
                    variant = variant,
                    contentPadding = 6.dp,
                    cornerRadius = ComicShapeTokens.ChipRadius,
                    shadowOffset = ComicShapeTokens.ShadowOffsetSmall,
                    borderWidth = ComicShapeTokens.BorderWidthThin,
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
                            colorFilter = ColorFilter.tint(ComicTokens.SolidBlack),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.displayName,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = ComicTokens.SolidBlack
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 25: ComicRecurrenceSelector matching PixelRecurrenceSelector's role.
 * Features Bangers label and ComicPanel chips with Burnt Orange selected fills.
 */
@Composable
fun ComicRecurrenceSelector(
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
            text = label.uppercase(),
            fontFamily = BangersFontFamily,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp,
            color = ComicTokens.SolidBlack,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (type, typeName) ->
                val isSelected = selectedType == type
                val variant = if (isSelected) ComicPanelVariant.BURNT_ORANGE else ComicPanelVariant.SURFACE

                ComicPanel(
                    variant = variant,
                    contentPadding = 8.dp,
                    cornerRadius = ComicShapeTokens.ChipRadius,
                    shadowOffset = ComicShapeTokens.ShadowOffsetSmall,
                    borderWidth = ComicShapeTokens.BorderWidthThin,
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
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = ComicTokens.SolidBlack
                        )
                    }
                }
            }
        }
    }
}
