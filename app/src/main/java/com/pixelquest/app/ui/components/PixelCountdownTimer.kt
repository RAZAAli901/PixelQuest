package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.PixelGold
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalTime

object CountdownFormatter {
    fun formatRemainingTime(scheduledTime: LocalTime, now: LocalTime = LocalTime.now()): String {
        val duration = Duration.between(now, scheduledTime)
        if (duration.isNegative || duration.isZero) {
            return "TIME'S UP!"
        }
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        return when {
            hours > 0 -> String.format("%02dh %02dm left", hours, minutes)
            minutes > 0 -> String.format("%02dm left", minutes)
            else -> String.format("%02ds left", seconds)
        }
    }

    fun isUrgent(scheduledTime: LocalTime, now: LocalTime = LocalTime.now(), thresholdMinutes: Long = 15): Boolean {
        val duration = Duration.between(now, scheduledTime)
        return !duration.isNegative && duration.toMinutes() < thresholdMinutes
    }

    fun isExpired(scheduledTime: LocalTime, now: LocalTime = LocalTime.now()): Boolean {
        return Duration.between(now, scheduledTime).isNegative
    }
}

@Composable
fun PixelCountdownTimer(
    scheduledTime: LocalTime,
    modifier: Modifier = Modifier,
    overrideNow: LocalTime? = null
) {
    var currentTime by remember { mutableStateOf(overrideNow ?: LocalTime.now()) }

    LaunchedEffect(overrideNow) {
        if (overrideNow == null) {
            while (true) {
                currentTime = LocalTime.now()
                delay(30_000L) // tick every 30 seconds
            }
        }
    }

    val now = overrideNow ?: currentTime
    val isExpired = CountdownFormatter.isExpired(scheduledTime, now)
    val isUrgent = CountdownFormatter.isUrgent(scheduledTime, now)
    val text = CountdownFormatter.formatRemainingTime(scheduledTime, now)

    val mode = com.pixelquest.app.ui.theme.PixelTheme.mode
    val isComic = mode == com.pixelquest.app.ui.theme.ThemeMode.Comic

    val textColor: Color = if (isComic) {
        when {
            isExpired -> com.pixelquest.app.ui.theme.ComicTokens.ErrorRed
            isUrgent -> com.pixelquest.app.ui.theme.ComicTokens.SolidBlack
            else -> com.pixelquest.app.ui.theme.ComicTokens.SolidBlack
        }
    } else {
        when {
            isExpired -> PixelRed
            isUrgent -> PixelGold
            else -> PixelCyan
        }
    }

    val borderColor: Color = if (isComic) {
        com.pixelquest.app.ui.theme.ComicTokens.SolidBlack
    } else {
        when {
            isExpired -> PixelRed
            isUrgent -> PixelGold
            else -> PixelCyan.copy(alpha = 0.6f)
        }
    }

    val backgroundColor: Color = if (isComic) {
        when {
            isExpired -> com.pixelquest.app.ui.theme.ComicTokens.CoralRed.copy(alpha = 0.2f)
            isUrgent -> com.pixelquest.app.ui.theme.ComicTokens.BurntOrange.copy(alpha = 0.3f)
            else -> com.pixelquest.app.ui.theme.ComicTokens.SkyBlue.copy(alpha = 0.25f)
        }
    } else {
        when {
            isExpired -> PixelRed.copy(alpha = 0.15f)
            isUrgent -> PixelGold.copy(alpha = 0.15f)
            else -> PixelBackgroundDark
        }
    }

    val timerShape = if (isComic) {
        androidx.compose.foundation.shape.RoundedCornerShape(com.pixelquest.app.ui.theme.ComicShapeTokens.ChipRadius)
    } else {
        CutCornerShape(2.dp)
    }
    val borderWidth = if (isComic) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .background(backgroundColor, shape = timerShape)
            .border(borderWidth, borderColor, timerShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
