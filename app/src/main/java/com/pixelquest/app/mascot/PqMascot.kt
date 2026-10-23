package com.pixelquest.app.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pix — animated mascot. Mirrors design-reference/PQMascot.dc.html:
 *  - pops (scale 0.55 → 1.12 → 1) whenever [mood] changes
 *  - whole-body idle motion per mood (bob / jump / shake / droop …)
 *  - per-layer accents (blink, flame flicker, sweat drip, Zzz float, twinkle, glow, crown shine)
 *
 * Usage: PqMascot(PixMood.Worried, size = 96.dp)
 * Notifications use PqMascotBitmap (static), never this.
 */
@Composable
fun PqMascot(
    mood: PixMood,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    animate: Boolean = true,
    pixel: Boolean = PqSkinState.skin == PqSkin.Pixel,
) {
    val frame = remember(mood, pixel) { PixFrames.frame(mood, pixel) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        if (!animate) return@LaunchedEffect
        pop.snapTo(0.55f)
        pop.animateTo(
            1f,
            keyframes {
                durationMillis = 450
                1.12f at 250
                0.96f at 360
            },
        )
    }
    val inf = rememberInfiniteTransition(label = "pix")
    // one master clock per period; each effect samples its own phase
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(motionPeriod(frame.motion), easing = LinearEasing)), label = "motion")
    val fast by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "fast")
    val blink by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "blink")

    Canvas(modifier.size(size)) {
        val cell = this.size.minDimension / 24f
        val m = if (animate) motion(frame.motion, t) else Motion()
        scale(pop.value, pivot = Offset(this.size.width / 2, this.size.height * 0.9f)) {
            translate(m.dx * this.size.width, m.dy * this.size.height) {
                rotate(m.rot, pivot = Offset(this.size.width / 2, this.size.height * 0.9f)) {
                    scale(m.sx, m.sy, pivot = Offset(this.size.width / 2, this.size.height * 0.9f)) {
                        frame.layers.forEach { layer -> drawLayer(layer, cell, if (animate) layer.anim else null, fast, blink) }
                    }
                }
            }
        }
    }
}

private data class Motion(val dx: Float = 0f, val dy: Float = 0f, val rot: Float = 0f, val sx: Float = 1f, val sy: Float = 1f)

private fun motionPeriod(m: String) = when (m) {
    "jump" -> 1600
    "bounce" -> 1100
    "shake" -> 2200
    "droop" -> 3000
    "breathe" -> 3200
    "lean" -> 2800
    "wiggle" -> 2000
    "sob" -> 600
    else -> 2400
}

private fun wave(t: Float) = sin(2 * PI * t).toFloat()

private fun motion(m: String, t: Float): Motion = when (m) {
    "bob" -> Motion(dy = -0.03f * (0.5f - 0.5f * kotlin.math.cos(2 * PI * t).toFloat()))
    "jump" -> when {
        t < 0.10f -> Motion(sx = 1.08f, sy = 0.92f)
        t < 0.22f -> {
            val p = (t - 0.10f) / 0.12f
            Motion(dy = -0.14f * p, sx = 0.95f, sy = 1.06f)
        }
        t < 0.36f -> {
            val p = (t - 0.22f) / 0.14f
            Motion(dy = -0.14f * (1 - p), sx = 1f + 0.06f * p, sy = 1f - 0.06f * p)
        }
        t < 0.45f -> Motion()
        else -> Motion()
    }
    "bounce" -> Motion(dy = -0.08f * maxOf(0f, wave(t * 1.5f)))
    "shake" -> if (t < 0.32f) Motion(dx = 0.03f * wave(t * 15f), rot = 2f * wave(t * 15f)) else Motion()
    "droop" -> Motion(dy = 0.04f * (0.5f - 0.5f * kotlin.math.cos(2 * PI * t).toFloat()), rot = -3f * (0.5f - 0.5f * kotlin.math.cos(2 * PI * t).toFloat()))
    "breathe" -> {
        val p = 0.5f - 0.5f * kotlin.math.cos(2 * PI * t).toFloat()
        Motion(dy = 0.015f * p, sx = 1f + 0.03f * p, sy = 1f - 0.03f * p)
    }
    "lean" -> {
        val p = 0.5f - 0.5f * kotlin.math.cos(2 * PI * t).toFloat()
        Motion(dx = -0.02f * p, rot = -5f * p)
    }
    "wiggle" -> if (t < 0.5f) Motion(rot = 7f * wave(t * 4f)) else Motion()
    "sob" -> Motion(dy = if (t < 0.5f) 0f else 0.02f)
    else -> Motion()
}

private fun DrawScope.drawLayer(layer: MascotLayer, cell: Float, anim: String?, fast: Float, blink: Float) {
    if (layer.cells.isEmpty()) return
    val minX = layer.cells.minOf { it.x }
    val maxX = layer.cells.maxOf { it.x } + 1
    val minY = layer.cells.minOf { it.y }
    val maxY = layer.cells.maxOf { it.y } + 1
    val center = Offset((minX + maxX) / 2f * cell, (minY + maxY) / 2f * cell)
    val bottom = Offset(center.x, maxY * cell)
    var alpha = 1f
    var dy = 0f
    var dx = 0f
    var sx = 1f
    var sy = 1f
    var pivot = center
    when (anim) {
        "blink" -> if (blink in 0.91f..0.97f) sy = 0.12f
        "flicker" -> {
            val p = (fast * 2.3f) % 1f
            sx = 1f - 0.06f * wave(p)
            sy = 1f + 0.1f * wave(p)
            pivot = bottom
        }
        "drip" -> {
            dy = 3f * cell * fast
            alpha = when {
                fast < 0.15f -> fast / 0.15f
                fast > 0.8f -> (1f - fast) / 0.2f
                else -> 1f
            }
        }
        "float" -> {
            dx = cell * fast
            dy = cell * (1f - 3f * fast)
            alpha = if (fast < 0.3f) fast / 0.3f else 1f - (fast - 0.3f) / 0.7f
        }
        "twinkle" -> alpha = if (((fast * 3.5f).toInt() % 2) == 0) 1f else 0.15f
        "glow" -> alpha = 0.72f + 0.28f * wave(fast)
        "shine" -> dy = -cell * (0.5f - 0.5f * wave(fast))
    }
    translate(dx, dy) {
        scale(sx, sy, pivot = pivot) {
            layer.cells.forEach { c ->
                drawRect(Color(c.color).copy(alpha = alpha * Color(c.color).alpha), topLeft = Offset(c.x * cell, c.y * cell), size = Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
