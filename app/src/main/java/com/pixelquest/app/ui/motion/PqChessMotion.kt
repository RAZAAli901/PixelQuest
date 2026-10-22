package com.pixelquest.app.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/*
 * Chess animations C01 to C09, from the owner's animations handoff (design-reference/PixelQuest
 * Animations.dc.html, section 03). Board-agnostic: callers pass square sizes and positions, and the
 * pieces are any composable. Budget: a piece moves in at most 150 ms (80 ms in 1-minute bullet), and
 * the clock is the server's, so an animation never costs anyone clock time.
 */

object ChessMotion {
    fun moveMs(bullet: Boolean) = if (bullet) 80 else 150
    val Red get() = if (PqMotion.pixel) Color(0xFFC25E00) else Color(0xFFFF5A4E)
}

// C01: the piece glides to its square with a lift and a landing squash. Key it by the piece's id.
@Composable
fun AnimatedPiece(col: Int, row: Int, square: Dp, modifier: Modifier = Modifier, bullet: Boolean = false, mine: Boolean = false, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val x = remember { Animatable(col.toFloat()) }
    val y = remember { Animatable(row.toFloat()) }
    val lift = remember { Animatable(1f) }
    val squash = remember { Animatable(1f) }
    LaunchedEffect(col, row) {
        if (x.value == col.toFloat() && y.value == row.toFloat()) return@LaunchedEffect
        if (reduced) { x.snapTo(col.toFloat()); y.snapTo(row.toFloat()); return@LaunchedEffect }
        val ms = ChessMotion.moveMs(bullet)
        launch { lift.animateTo(1.14f, tween(ms / 3)) }
        launch { x.animateTo(col.toFloat(), tween(ms, easing = PqMotion.out())) }
        y.animateTo(row.toFloat(), tween(ms, easing = PqMotion.out()))
        lift.snapTo(1f)
        if (mine) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (!bullet && !PqMotion.pixel) squash.animateTo(1f, keyframes { durationMillis = 140; 0.88f at 50 })
    }
    Box(
        modifier.size(square)
            .graphicsLayer {
                translationX = x.value * square.toPx()
                translationY = y.value * square.toPx() - (lift.value - 1f) * 30f
                scaleX = lift.value * (2f - squash.value)
                scaleY = lift.value * squash.value
                transformOrigin = TransformOrigin(0.5f, 0.9f)
                shadowElevation = if (lift.value > 1f && !PqMotion.pixel) 6f else 0f
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

// C02: the captured piece pops, shards burst, and it flies off towards its tray.
@Composable
fun CaptureEffect(col: Int, row: Int, square: Dp, trayOffset: Offset, onDone: () -> Unit = {}, content: @Composable () -> Unit) {
    val reduced = reducedMotion()
    val t = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        if (reduced) { onDone(); return@LaunchedEffect }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        t.animateTo(1f, tween(650, easing = LinearEasing))
        onDone()
    }
    if (reduced || t.value >= 1f) return
    val colors = listOf(PqColors.Gold, ChessMotion.Red, PqColors.Sky)
    Box(Modifier.offset(square * col, square * row).size(square)) {
        Canvas(Modifier.matchParentSize()) {
            val k = (t.value / 0.4f).coerceIn(0f, 1f)
            if (k in 0.01f..0.99f) for (i in 0 until 8) {
                val a = (i * Math.PI / 4).toFloat()
                val d = 10.dp.toPx() + 32.dp.toPx() * k
                val s = 8.dp.toPx() * (1f - 0.7f * k)
                val c = Offset(center.x + cos(a) * d, center.y + sin(a) * d)
                drawRect(colors[i % 3].copy(alpha = 1f - k), topLeft = c - Offset(s / 2, s / 2), size = Size(s, s))
            }
        }
        Box(Modifier.matchParentSize().graphicsLayer {
            val pop = if (t.value < 0.15f) 1f + 0.35f * (t.value / 0.15f) else 1.35f - 0.8f * ((t.value - 0.15f) / 0.85f)
            val fly = ((t.value - 0.15f) / 0.85f).coerceIn(0f, 1f)
            scaleX = pop; scaleY = pop; rotationZ = 12f + 348f * fly
            translationX = trayOffset.x * fly; translationY = trayOffset.y * fly
            alpha = if (t.value > 0.95f) (1f - t.value) * 20f else 1f
        }, contentAlignment = Alignment.Center) { content() }
    }
}

/** Bumps the captured tray when [count] grows, as the captured piece arrives. */
fun Modifier.trayBump(count: Int): Modifier = composed {
    val reduced = reducedMotion()
    val s = remember { Animatable(1f) }
    var last by remember { mutableStateOf(count) }
    LaunchedEffect(count) {
        val grew = count > last
        last = count
        if (grew && !reduced) { delay(400); s.animateTo(1f, keyframes { durationMillis = 220; 1.25f at 90 }) }
    }
    graphicsLayer { scaleX = s.value; scaleY = s.value }
}

// C03: check. The king's square pulses red, the king shakes, and CHECK! pops up.
fun Modifier.checkPulse(inCheck: Boolean): Modifier = composed {
    if (!inCheck) return@composed this
    if (reducedMotion()) return@composed drawBehind { drawRect(ChessMotion.Red.copy(alpha = 0.6f)) }
    val inf = rememberInfiniteTransition(label = "check")
    val a by inf.animateFloat(
        0.2f, 1f,
        infiniteRepeatable(tween(500, easing = if (PqMotion.pixel) PqMotion.steps(2) else LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    drawBehind { drawRect(ChessMotion.Red.copy(alpha = a)) }
}

fun Modifier.kingShake(trigger: Int): Modifier = composed {
    val reduced = reducedMotion()
    val x = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(trigger) {
        if (trigger > 0 && !reduced) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            x.animateTo(0f, keyframes { durationMillis = 300; -3f at 50; 3f at 100; -3f at 150; 3f at 200 })
        }
    }
    graphicsLayer { translationX = x.value.dp.toPx(); rotationZ = x.value * 1.6f }
}

@Composable
fun CheckTag(trigger: Int, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val s = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) { s.snapTo(0f); return@LaunchedEffect }
        if (reduced) s.snapTo(1f) else s.animateTo(1f, keyframes { durationMillis = 240; 1.25f at 140 })
        delay(1200)
        if (reduced) s.snapTo(0f) else s.animateTo(0f, tween(PqMotion.Fast))
    }
    if (s.value > 0f) {
        Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; rotationZ = -8f }.comic(ChessMotion.Red, 8.dp).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(stringResource(R.string.motion_check), style = PqType.display(24).copy(color = if (PqMotion.pixel) PqColors.Paper else PqColors.Ink))
        }
    }
}

// C04: checkmate. The king topples, the board dims, and the stamp lands.
fun Modifier.kingTopple(mated: Boolean): Modifier = composed {
    val reduced = reducedMotion()
    val r = remember { Animatable(if (mated) 90f else 0f) }
    LaunchedEffect(mated) {
        when {
            !mated -> r.snapTo(0f)
            reduced -> r.snapTo(90f)
            r.value != 90f -> { r.animateTo(-14f, tween(250)); r.animateTo(100f, tween(280, easing = PqMotion.out())); r.animateTo(90f, tween(140)) }
        }
    }
    graphicsLayer { rotationZ = r.value; transformOrigin = TransformOrigin(0.8f, 0.9f) }
}

@Composable
fun BoardDim(on: Boolean, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val a by animateFloatAsState(if (on) 0.4f else 0f, if (reduced) snap() else tween(PqMotion.Slow, delayMillis = 450), label = "dim")
    if (a > 0f) Box(modifier.fillMaxSize().graphicsLayer { alpha = a }.background(PqColors.Ink))
}

@Composable
fun ResultStamp(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val s = remember { Animatable(2.6f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        when {
            !visible -> { s.snapTo(2.6f); a.snapTo(0f) }
            reduced -> { s.snapTo(1f); a.snapTo(1f) }
            else -> {
                delay(650)
                launch { a.animateTo(1f, tween(PqMotion.Fast)) }
                s.animateTo(1f, keyframes { durationMillis = 300; 0.9f at 230 })
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    if (a.value > 0f) {
        Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = a.value; rotationZ = -8f }
            .background(PqColors.Paper, pqShape(8.dp)).border(3.dp, ChessMotion.Red, pqShape(8.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
            Text(text, style = PqType.display(30).copy(color = ChessMotion.Red))
        }
    }
}

// C05: a clock running low flashes, ticks and shakes each second (harder under 5 seconds).
fun Modifier.clockAlarm(millis: Long, low: Boolean): Modifier = composed {
    val reduced = reducedMotion()
    val sec = (millis / 1000).toInt()
    val flash = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(sec, low) {
        if (!low || reduced) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        launch { flash.snapTo(1f); flash.animateTo(0f, tween(350)) }
        val amp = if (millis < 5000) 4f else 3f
        shake.animateTo(0f, keyframes { durationMillis = 220; -amp at 60; amp at 120; -amp / 2 at 170 })
    }
    graphicsLayer { translationX = shake.value.dp.toPx(); val s = 1f + 0.06f * flash.value; scaleX = s; scaleY = s }
}

// C06: the selected piece lifts, and the legal moves pop in one after another.
fun Modifier.pieceLift(selected: Boolean): Modifier = composed {
    val reduced = reducedMotion()
    val y by animateFloatAsState(if (selected) -6f else 0f, if (reduced) snap() else tween(PqMotion.Fast, easing = PqMotion.snap()), label = "lift")
    graphicsLayer { translationY = y.dp.toPx(); val s = 1f + (-y / 6f) * 0.1f; scaleX = s; scaleY = s }
}

/** One legal move's mark: a dot, or a ring on a capture; [order] staggers the pop. */
@Composable
fun MoveDot(capture: Boolean, order: Int, color: Color, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val s = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(order * 60L)
        s.animateTo(1f, keyframes { durationMillis = PqMotion.Base; 1.35f at 140 })
    }
    Canvas(modifier.fillMaxSize().graphicsLayer { scaleX = s.value; scaleY = s.value }) {
        when {
            capture -> drawCircle(color, size.minDimension * 0.42f, style = Stroke(size.minDimension * 0.08f))
            PqMotion.pixel -> drawRect(color, topLeft = center - Offset(6.dp.toPx(), 6.dp.toPx()), size = Size(12.dp.toPx(), 12.dp.toPx()))
            else -> drawCircle(color, 6.dp.toPx())
        }
    }
}

// C07: the result. The Int change pops in, and the head-to-head cell that went up flashes.
@Composable
fun IntDeltaChip(text: String, good: Boolean, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val s = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(text) {
        if (reduced) return@LaunchedEffect
        delay(200)
        s.animateTo(1f, keyframes { durationMillis = PqMotion.Base; 1.2f at 150 })
    }
    Box(
        modifier.graphicsLayer { scaleX = s.value; scaleY = s.value }.clip(PqCircle).background(if (good) PqColors.Progress else ChessMotion.Red)
            .border(2.dp, PqColors.Ink, PqCircle).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = PqType.Button.copy(color = PqColors.Paper))
    }
}

/** A head-to-head cell's flash when it's the one that went up. */
fun Modifier.cellFlash(on: Boolean): Modifier = composed {
    val reduced = reducedMotion()
    val flash = remember { Animatable(0f) }
    LaunchedEffect(on) { if (on && !reduced) { delay(700); flash.snapTo(1f); flash.animateTo(0f, tween(500)) } }
    drawBehind { if (flash.value > 0f) drawRect(PqColors.Paper.copy(alpha = flash.value)) }
}

// C08: the challenge. The two players slide in and clash, and the time left drains.
@Composable
fun ChallengeClash(me: @Composable () -> Unit, them: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val k = remember { Animatable(if (reduced) 1f else 0f) }
    val vs = remember { Animatable(if (reduced) 1f else 0f) }
    val shake = remember { Animatable(0f) }
    var burst by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        k.animateTo(1f, tween(PqMotion.Slow, easing = PqMotion.snap()))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        burst = 1
        launch { vs.animateTo(1f, keyframes { durationMillis = 260; 1.4f at 140 }) }
        shake.animateTo(0f, keyframes { durationMillis = 240; -4f at 40; 4f at 100; -2f at 170 })
    }
    Row(
        modifier.graphicsLayer { translationX = shake.value.dp.toPx() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(Modifier.graphicsLayer { translationX = -130.dp.toPx() * (1f - k.value) }) { me() }
        RewardBurst(trigger = burst, shards = 8) {
            Text(stringResource(R.string.motion_vs), style = PqType.display(36).copy(color = ChessMotion.Red), modifier = Modifier.graphicsLayer { scaleX = vs.value; scaleY = vs.value })
        }
        Box(Modifier.graphicsLayer { translationX = 130.dp.toPx() * (1f - k.value) }) { them() }
    }
}

@Composable
fun ExpiryBar(fraction: Float, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val p by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        if (reduced) snap() else tween(1000, easing = if (PqMotion.pixel) PqMotion.steps(2) else LinearEasing),
        label = "exp"
    )
    Box(modifier.fillMaxWidth().height(12.dp).clip(PqCircle).background(PqColors.Sunken).border(2.dp, PqColors.Ink, PqCircle)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(ChessMotion.Red))
    }
}

// C09: promotion. The pawn shrinks out, a flash, and the new piece spins in with rays.
@Composable
fun PromotionMorph(trigger: Int, square: Dp, pawn: @Composable () -> Unit, promoted: @Composable () -> Unit) {
    val reduced = reducedMotion()
    val idle = trigger == 0 || reduced
    val out = remember(trigger) { Animatable(if (idle) 0f else 1f) }
    val inn = remember(trigger) { Animatable(if (idle) 1f else 0f) }
    val flash = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (idle) return@LaunchedEffect
        launch { flash.snapTo(1f); flash.animateTo(0f, tween(300)) }
        out.animateTo(0f, tween(PqMotion.Fast))
        inn.animateTo(1f, keyframes { durationMillis = 300; 1.35f at 180 })
    }
    Box(Modifier.size(square).drawBehind { if (flash.value > 0f) drawRect(Color.White.copy(alpha = flash.value)) }, contentAlignment = Alignment.Center) {
        if (out.value > 0f) Box(Modifier.graphicsLayer { scaleX = out.value; scaleY = out.value; alpha = out.value }) { pawn() }
        RewardBurst(trigger = if (idle) 0 else trigger, shards = 8) {
            Box(Modifier.graphicsLayer { scaleX = inn.value; scaleY = inn.value; rotationZ = (1f - inn.value) * -40f }) { promoted() }
        }
    }
}
