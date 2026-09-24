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

    // Comic-mode dedicated ramp (signature container colors + solid black ink borders)
    val ComicEmptyCell = Color(0xFFF4EFE6) // SurfaceVariant newsprint
    val ComicEmptyBorder = Color(0xFF000000)
    val ComicPerfectCell = Color(0xFF8ECAE6) // Sky Blue signature container
    val ComicPerfectBorder = Color(0xFF000000)
    val ComicPartialCell = Color(0xFFF0A868) // Burnt Orange signature container
    val ComicPartialBorder = Color(0xFF000000)
    val ComicMissedCell = Color(0xFFFF5A4E) // Coral Red accent
    val ComicMissedBorder = Color(0xFF000000)
    val ComicBonusCell = Color(0xFFB8A4D4) // Lavender signature container

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

    fun getCellColor(status: DailyStatus, mode: com.pixelquest.app.ui.theme.ThemeMode): Color {
        return when (mode) {
            com.pixelquest.app.ui.theme.ThemeMode.Comic -> when (status) {
                DailyStatus.PERFECT -> ComicPerfectCell
                DailyStatus.PARTIAL -> ComicPartialCell
                DailyStatus.MISSED -> ComicMissedCell
                DailyStatus.NO_TASKS_SCHEDULED -> ComicEmptyCell
            }
            com.pixelquest.app.ui.theme.ThemeMode.Light -> when (status) {
                DailyStatus.PERFECT -> LightPerfectCell
                DailyStatus.PARTIAL -> LightPartialCell
                DailyStatus.MISSED -> LightMissedCell
                DailyStatus.NO_TASKS_SCHEDULED -> LightEmptyCell
            }
            else -> when (status) {
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

    fun getBorderColor(status: DailyStatus, mode: com.pixelquest.app.ui.theme.ThemeMode): Color {
        return when (mode) {
            com.pixelquest.app.ui.theme.ThemeMode.Comic -> ComicEmptyBorder
            com.pixelquest.app.ui.theme.ThemeMode.Light -> when (status) {
                DailyStatus.PERFECT -> LightPerfectBorder
                DailyStatus.PARTIAL -> LightPartialBorder
                DailyStatus.MISSED -> LightMissedBorder
                DailyStatus.NO_TASKS_SCHEDULED -> LightEmptyBorder
            }
            else -> when (status) {
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
    isLightOverride: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode
    val isComic = activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic && isLightOverride != true

    val fillColor = if (isComic) {
        HeatmapColorMapper.getCellColor(status, com.pixelquest.app.ui.theme.ThemeMode.Comic)
    } else {
        val isLight = isLightOverride ?: (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Light)
        HeatmapColorMapper.getCellColor(status, isLight = isLight)
    }

    val borderColor = if (isComic) {
        HeatmapColorMapper.getBorderColor(status, com.pixelquest.app.ui.theme.ThemeMode.Comic)
    } else {
        val isLight = isLightOverride ?: (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Light)
        HeatmapColorMapper.getBorderColor(status, isLight = isLight)
    }

    val cornerRadius = if (isComic) 3.dp else 2.dp
    val borderWidth = if (isComic) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .size(size)
            .background(fillColor, shape = RoundedCornerShape(cornerRadius))
            .border(borderWidth, borderColor, shape = RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    )
}
