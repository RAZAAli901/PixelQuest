package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelGold

object HeatmapColorMapper {
    fun getCellColor(status: DailyStatus): Color {
        return when (status) {
            DailyStatus.PERFECT -> PixelGreen
            DailyStatus.PARTIAL -> PixelGold
            DailyStatus.MISSED -> PixelRed
            DailyStatus.NO_TASKS_SCHEDULED -> PixelSurfaceDark
        }
    }

    fun getBorderColor(status: DailyStatus): Color {
        return when (status) {
            DailyStatus.PERFECT -> Color(0xFF1C7139)
            DailyStatus.PARTIAL -> Color(0xFFC8A100)
            DailyStatus.MISSED -> Color(0xFF8B0000)
            DailyStatus.NO_TASKS_SCHEDULED -> Color(0xFF333842)
        }
    }
}

@Composable
fun PixelHeatmapCell(
    status: DailyStatus,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    onClick: (() -> Unit)? = null
) {
    val fillColor = HeatmapColorMapper.getCellColor(status)
    val borderColor = HeatmapColorMapper.getBorderColor(status)

    Box(
        modifier = modifier
            .size(size)
            .background(fillColor, shape = RoundedCornerShape(2.dp))
            .border(1.dp, borderColor, shape = RoundedCornerShape(2.dp))
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    )
}
