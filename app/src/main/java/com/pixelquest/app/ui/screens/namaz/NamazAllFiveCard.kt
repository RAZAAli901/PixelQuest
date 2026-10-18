package com.pixelquest.app.ui.screens.namaz

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.inkOnPanel
import com.pixelquest.app.ui.theme.rememberReduceMotion
import kotlin.math.cos
import kotlin.math.sin

/** The day's fifth prayer was just marked: what it unlocked. */
data class NamazAllFive(val allFiveBonus: Int, val weekBonus: Int)

/**
 * "All five prayed!", with the bonus it earned, over a burst in the theme's style: square pixels
 * in Pixel, confetti with ink outlines in Comic, soft dots in Light. Reduced motion shows the card
 * without the burst. Without XP text ([showXp] false: Simple Mode, or Namaz XP off) it celebrates
 * the prayers alone.
 */
@Composable
fun NamazAllFiveCard(allFive: NamazAllFive, showXp: Boolean, onDismiss: () -> Unit) {
    val colors = PixelTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.testTag(NamazTestTags.ALL_FIVE), contentAlignment = Alignment.Center) {
            PixelCard(variant = PixelPanelVariant.BEIGE, contentPadding = namazStyle().cardPadding, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🕌", style = MaterialTheme.typography.displaySmall)
                    Text(
                        text = stringResource(R.string.namaz_all_five_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = inkOnPanel(colors.primaryText),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.namaz_all_five_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (showXp && allFive.allFiveBonus > 0) {
                        Text(
                            text = stringResource(R.string.namaz_all_five_bonus, allFive.allFiveBonus),
                            style = MaterialTheme.typography.titleMedium,
                            color = inkOnPanel(colors.primaryText)
                        )
                    }
                    if (showXp && allFive.weekBonus > 0) {
                        Text(
                            text = stringResource(R.string.namaz_week_bonus, allFive.weekBonus),
                            style = MaterialTheme.typography.titleSmall,
                            color = inkOnPanel(colors.primaryText),
                            textAlign = TextAlign.Center
                        )
                    }
                    PixelButton(text = stringResource(R.string.namaz_all_five_done), onClick = onDismiss, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Burst(modifier = Modifier.matchParentSize())
        }
    }
}

/** Once, outward from the middle and fading: pixels, confetti or dots, by theme. */
@Composable
private fun Burst(modifier: Modifier) {
    if (rememberReduceMotion(LocalReduceMotion.current)) return
    val mode = PixelTheme.mode
    val colors = PixelTheme.colors
    val palette = when (mode) {
        ThemeMode.Comic -> listOf(ComicTokens.CoralRed, ComicTokens.SkyBlue, ComicTokens.GoldAccent, ComicTokens.Lavender)
        else -> listOf(colors.primary, colors.tertiary, colors.secondary, colors.gold)
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 1_400, easing = FastOutSlowInEasing)) }
    Canvas(modifier = modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val reach = size.minDimension * 0.75f
        val pieces = 28
        for (i in 0 until pieces) {
            val angle = Math.toRadians(i * 360.0 / pieces + (i % 3) * 7.0)
            val distance = reach * t * (0.6f + (i % 4) * 0.13f)
            val center = Offset(size.width / 2 + (cos(angle) * distance).toFloat(), size.height / 2 + (sin(angle) * distance).toFloat())
            val color = palette[i % palette.size].copy(alpha = 1f - t)
            when (mode) {
                ThemeMode.Comic -> rotate(degrees = i * 37f + t * 180f, pivot = center) {
                    val piece = Size(14f, 8f)
                    val topLeft = Offset(center.x - piece.width / 2, center.y - piece.height / 2)
                    drawRect(color = color, topLeft = topLeft, size = piece)
                    drawRect(color = Color.Black.copy(alpha = 1f - t), topLeft = topLeft, size = piece, style = Stroke(width = 2f))
                }
                ThemeMode.Pixel, ThemeMode.System -> drawRect(color = color, topLeft = Offset(center.x - 6f, center.y - 6f), size = Size(12f, 12f))
                else -> drawCircle(color = color, radius = 6f, center = center)
            }
        }
    }
}
