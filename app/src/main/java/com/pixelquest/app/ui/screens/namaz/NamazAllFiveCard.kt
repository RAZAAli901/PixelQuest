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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqChip
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.isPixelSkin
import com.pixelquest.app.ui.theme.rememberReduceMotion
import kotlin.math.cos
import kotlin.math.sin

/** The day's fifth prayer was just marked: what it unlocked. */
data class NamazAllFive(val allFiveBonus: Int, val weekBonus: Int)

/**
 * "All five prayed!", with the bonus it earned, over a burst in the skin's style: square pixels in
 * Pixel, inked confetti in Comic. Reduced motion shows the card without the burst. Without XP text
 * ([showXp] false: Simple Mode, or Namaz XP off) it celebrates the prayers alone.
 */
@Composable
fun NamazAllFiveCard(allFive: NamazAllFive, showXp: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.testTag(NamazTestTags.ALL_FIVE), contentAlignment = Alignment.Center) {
            PqCard(Modifier.fillMaxWidth(), shadow = PqDimens.ShadowLg, radius = 18.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryTile(Icons.Rounded.Mosque, PqColors.Sky, size = 56.dp, iconSize = 32.dp, radius = 14.dp)
                    Text(text = stringResource(R.string.namaz_all_five_title), style = PqType.CardTitle, textAlign = TextAlign.Center)
                    Text(text = stringResource(R.string.namaz_all_five_body), style = PqType.Body.copy(color = PqColors.Ink2), textAlign = TextAlign.Center)
                    if (showXp && allFive.allFiveBonus > 0) PqChip(stringResource(R.string.namaz_all_five_bonus, allFive.allFiveBonus), PqColors.Gold)
                    if (showXp && allFive.weekBonus > 0) {
                        Text(text = stringResource(R.string.namaz_week_bonus, allFive.weekBonus), style = PqType.BodyStrong, textAlign = TextAlign.Center)
                    }
                    PqButton(text = stringResource(R.string.namaz_all_five_done), onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
            }
            Burst(modifier = Modifier.matchParentSize())
        }
    }
}

/** Once, outward from the middle and fading: square pixels in Pixel, inked confetti in Comic. */
@Composable
private fun Burst(modifier: Modifier) {
    if (rememberReduceMotion(LocalReduceMotion.current)) return
    val pixel = isPixelSkin
    val palette = listOf(PqColors.Red, PqColors.Sky, PqColors.Gold, PqColors.Lilac)
    val ink = PqColors.Ink
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
            if (pixel) {
                drawRect(color = color, topLeft = Offset(center.x - 6f, center.y - 6f), size = Size(12f, 12f))
            } else {
                rotate(degrees = i * 37f + t * 180f, pivot = center) {
                    val piece = Size(14f, 8f)
                    val topLeft = Offset(center.x - piece.width / 2, center.y - piece.height / 2)
                    drawRect(color = color, topLeft = topLeft, size = piece)
                    drawRect(color = ink.copy(alpha = 1f - t), topLeft = topLeft, size = piece, style = Stroke(width = 2f))
                }
            }
        }
    }
}
