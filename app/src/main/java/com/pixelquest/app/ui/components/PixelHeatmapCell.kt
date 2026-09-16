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

/**
 * Dedicated color mapper for PixelQuest heatmap cells.
 * Provides distinct, tailored color ramps for Pixel (dark arcade) and Light (paper daylight) themes.
 */
object HeatmapColorMapper {
    // Light-mode dedicated ramp (adapted for daylight contrast on white surfaces)
    val LightEmptyCell = Color(0xFFEFECE6)
    val LightEmptyBorder = Color(0xFFD5CEBF)
    val LightPerfectCell = Color(0xFF15803D)
    val LightPerfectBorder = Color(0xFF166534)
    val LightPartialCell = Color(0xFFD97706)
    val LightPartialBorder = Color(0xFFB45309)
    val LightMissedCell = Color(0xFFDC2626)
    val LightMissedBorder = Color(0xFF991B1B)

    // Dark-mode (Pixel) canonical 8-bit ramp
    val DarkEmptyCell = PixelSurfaceDark
    val DarkEmptyBorder = Color(0xFF333842)
    val DarkPerfectCell = PixelGreen
    val DarkPerfectBorder = Color(0xFF1C7139)
    val DarkPartialCell = PixelGold
    val DarkPartialBorder = Color(0xFFC8A100)
    val DarkMissedCell = PixelRed
    val DarkMissedBorder = Color(0xFF8B0000)

    fun getCellColor(status: DailyStatus, isLight: Boolean = false): Color {
        return if (isLight) {
            when (status) {
                DailyStatus.PERFECT -> LightPerfectCell
                DailyStatus.PARTIAL -> LightPartialCell
                DailyStatus.MISSED -> LightMissedCell
                DailyStatus.NO_TASKS_SCHEDULED -> LightEmptyCell
            }
        } else {
            when (status) {
                DailyStatus.PERFECT -> DarkPerfectCell
                DailyStatus.PARTIAL -> DarkPartialCell
                DailyStatus.MISSED -> DarkMissedCell
                DailyStatus.NO_TASKS_SCHEDULED -> DarkEmptyCell
            }
        }
    }

    fun getBorderColor(status: DailyStatus, isLight: Boolean = false): Color {
        return if (isLight) {
            when (status) {
                DailyStatus.PERFECT -> LightPerfectBorder
                DailyStatus.PARTIAL -> LightPartialBorder
                DailyStatus.MISSED -> LightMissedBorder
                DailyStatus.NO_TASKS_SCHEDULED -> LightEmptyBorder
            }
        } else {
            when (status) {
                DailyStatus.PERFECT -> DarkPerfectBorder
                DailyStatus.PARTIAL -> DarkPartialBorder
                DailyStatus.MISSED -> DarkMissedBorder
                DailyStatus.NO_TASKS_SCHEDULED -> DarkEmptyBorder
            }
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
