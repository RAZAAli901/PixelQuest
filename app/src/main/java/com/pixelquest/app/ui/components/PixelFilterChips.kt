package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.screens.history.HistoryFilter
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelFilterChips(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        listOf(
            HistoryFilter.LAST_7_DAYS to "7 DAYS",
            HistoryFilter.LAST_30_DAYS to "30 DAYS",
            HistoryFilter.ALL_TIME to "ALL TIME"
        ).forEach { (filter, label) ->
            val isSelected = selectedFilter == filter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) PixelGold else PixelSurfaceDark,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) PixelGold else PixelTextWhite.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = PixelTypography.labelSmall.copy(fontSize = 9.sp),
                    color = if (isSelected) PixelBackgroundDark else PixelTextWhite
                )
            }
        }
    }
}
