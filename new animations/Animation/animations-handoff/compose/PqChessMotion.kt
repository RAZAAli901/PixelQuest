package com.pixelquest.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/*
 * Chess animations C01–C09 (design-reference/PixelQuest Animations.dc.html §03).
 * Board-agnostic: you pass square sizes/offsets; pieces are any composable (glyph, vector, image).
 * Budget: piece moves ≤150ms (80ms in 1-min bullet) so animation never costs clock time.
 */

object ChessMotion {
    fun moveMs(bullet: Boolean) = if (bullet) 80 else 150
    val Red get() = if (PqMotion.pixel) Color(0xFFC25E00) else Color(0xFFFF5A4E)
    val Highlight get() = if (PqMotion.pixel) Color(0xFFFDE7A0) else Color(0xFFFFD95A)
}

// C01 — piece glides with lift + landing squash. Key the composable by piece id so moves animate.
@Composable
fun AnimatedPiece(col: Int, row: Int, square: Dp, bullet: Boolean = false, mine: Boolean = false, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
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
                translationX = x.value * square.toPx(); translationY = y.value * square.toPx() - (lift.value - 1f) * 30f
                scaleX = lift.value * (2f - squash.value); scaleY = lift.value * squash.value
                transformOrigin = TransformOrigin(0.5f, 0.9f)
                shadowElevation = if (lift.value > 1f && !PqMotion.pixel) 6f else 0f
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun LastMoveHighlight(from: Pair<Int, Int>?, to: Pair<Int, Int>?, square: Dp) {
    val a by animateFloatAsState(if (to != null) 1f else 0f, tween(PqMotion.Fast), label = "hl")
    listOfNotNull(from, to).forEach { (c, r) ->
        Box(Modifier.offset(square * c, square * r).size(square).graphicsLayer { alpha = a }.background(ChessMotion.Highlight))
    }
}

// C02 — captured piece pops, shards burst, then flies to the tray.
@Composable
fun CaptureEffect(trigger: Int, col: Int, row: Int, square: Dp, trayOffset: Offset, content: @Composable () -> Unit) {
    if (trigger == 0) return
    val t = remember(trigger) { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(trigger) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); t.animateTo(1f, tween(650, easing = LinearEasing)) }
    val colors = listOf(PqColors.Gold, ChessMotion.Red, PqColors.Sky)
    Box(Modifier.offset(square * col, square * row).size(square)) {
        Canvas(Modifier.matchParentSize()) {
            val k = (t.value / 0.4f).coerceIn(0f, 1f)
            if (k in 0.01f..0.99f) for (i in 0 until 8) {
                val a = (i * Math.PI / 4).toFloat(); val d = 10.dp.toPx() + 32.dp.toPx() * k; val s = 8.dp.toPx() * (1f - 0.7f * k)
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

/** Bump the tray when [count] grows. */
fun Modifier.trayBump(count: Int): Modifier = composed {
    val s = remember { Animatable(1f) }
    LaunchedEffect(count) { if (count > 0) { delay(400); s.animateTo(1f, keyframes { durationMillis = 220; 1.25f at 90 }) } }
    graphicsLayer { scaleX = s.value; scaleY = s.value }
}

// C03 — check: pulsing red square + king shake.
fun Modifier.checkPulse(inCheck: Boolean): Modifier = composed {
    if (!inCheck) return@composed this
    val inf = rememberInfiniteTransition(label = "check")
    val a by inf.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, easing = if (PqMotion.pixel) PqMotion.steps(2) else LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "a")
    drawBehind { drawRect(ChessMotion.Red.copy(alpha = a)) }
}

fun Modifier.kingShake(trigger: Int): Modifier = composed {
    val x = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(trigger) { if (trigger > 0) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); x.animateTo(0f, keyframes { durationMillis = 300; -3f at 50; 3f at 100; -3f at 150; 3f at 200 }) } }
    graphicsLayer { translationX = x.value.dp.toPx(); rotationZ = x.value * 1.6f }
}

@Composable
fun CheckTag(visible: Boolean, modifier: Modifier = Modifier) {
    val s = remember { Animatable(0f) }
    LaunchedEffect(visible) { if (visible) { s.animateTo(1f, keyframes { durationMillis = 240; 1.25f at 140 }); delay(1200); s.animateTo(0f, tween(PqMotion.Fast)) } else s.snapTo(0f) }
    if (s.value > 0f) Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; rotationZ = -8f }.comic(ChessMotion.Red, 8.dp).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text("CHECK!", style = PqType.display(24).copy(color = if (PqMotion.pixel) PqColors.Paper else PqColors.Ink))
    }
}

// C04 — checkmate: king topples, board dims, stamp.
fun Modifier.kingTopple(mated: Boolean): Modifier = composed {
    val r = remember { Animatable(0f) }
    LaunchedEffect(mated) {
        if (!mated) { r.snapTo(0f); return@LaunchedEffect }
        r.animateTo(-14f, tween(250)); r.animateTo(100f, tween(280, easing = PqMotion.out())); r.animateTo(90f, tween(140))
    }
    graphicsLayer { rotationZ = r.value; transformOrigin = TransformOrigin(0.8f, 0.9f) }
}

@Composable
fun BoardDim(on: Boolean, modifier: Modifier = Modifier) {
    val a by animateFloatAsState(if (on) 0.4f else 0f, tween(PqMotion.Slow, delayMillis = 450), label = "dim")
    Box(modifier.fillMaxSize().graphicsLayer { alpha = a }.background(PqColors.Ink))
}

@Composable
fun ResultStamp(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val s = remember { Animatable(2.6f) }; val a = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (!visible) { s.snapTo(2.6f); a.snapTo(0f); return@LaunchedEffect }
        delay(650); launch { a.animateTo(1f, tween(PqMotion.Fast)) }
        s.animateTo(1f, keyframes { durationMillis = 300; 0.9f at 230 }); haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = a.value; rotationZ = -8f }
        .background(PqColors.Paper, pqShape(8.dp)).border(3.dp, ChessMotion.Red, pqShape(8.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text, style = PqType.display(30).copy(color = ChessMotion.Red))
    }
}

// C05 — clock with low-time flash, tick roll and shake.
@Composable
fun ChessClock(millis: Long, active: Boolean, modifier: Modifier = Modifier, lowTimeMs: Long = 10_000, style: TextStyle = PqType.display(30)) {
    val low = active && millis in 1 until lowTimeMs
    val sec = (millis / 1000).toInt()
    val flash = remember { Animatable(0f) }; val shake = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(sec, low) {
        if (!low) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        launch { flash.snapTo(1f); flash.animateTo(0f, tween(350)) }
        val amp = if (millis < 5000) 4f else 3f
        shake.animateTo(0f, keyframes { durationMillis = 220; -amp at 60; amp at 120; -amp / 2 at 170 })
    }
    val bg = if (active) PqColors.Ink else PqColors.Paper
    val fg = if (active) PqColors.Paper else PqColors.Ink
    Box(
        modifier.graphicsLayer { translationX = shake.value.dp.toPx(); val s = 1f + 0.06f * flash.value; scaleX = s; scaleY = s }
            .clip(pqShape(10.dp)).background(bg).drawBehind { if (flash.value > 0f) drawRect(ChessMotion.Red.copy(alpha = flash.value)) }
            .border(PqDimens.Border, PqColors.Ink, pqShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        RollingNumber(sec, style.copy(color = fg), format = { s -> if (millis < 20_000) "%d:%02d.%d".format(s / 60, s % 60, (millis % 1000) / 100) else "%d:%02d".format(s / 60, s % 60) })
    }
}

// C06 — selected piece lift + legal move dots (staggered pop).
fun Modifier.pieceLift(selected: Boolean): Modifier = composed {
    val y by animateFloatAsState(if (selected) -6f else 0f, tween(PqMotion.Fast, easing = PqMotion.snap()), label = "lift")
    graphicsLayer { translationY = y.dp.toPx(); val s = 1f + (-y / 6f) * 0.1f; scaleX = s; scaleY = s }
}

@Composable
fun LegalMoveDots(targets: List<Pair<Int, Int>>, captures: Set<Pair<Int, Int>>, square: Dp) {
    targets.forEachIndexed { i, (c, r) ->
        val s = remember(targets) { Animatable(0f) }
        LaunchedEffect(targets) { delay(i * 60L); s.animateTo(1f, keyframes { durationMillis = PqMotion.Base; 1.35f at 140 }) }
        Canvas(Modifier.offset(square * c, square * r).size(square).graphicsLayer { scaleX = s.value; scaleY = s.value }) {
            val dot = Color(0x66161412)
            if ((c to r) in captures) drawCircle(dot, size.minDimension * 0.42f, style = Stroke(size.minDimension * 0.08f))
            else if (PqMotion.pixel) drawRect(dot, topLeft = center - Offset(6.dp.toPx(), 6.dp.toPx()), size = Size(12.dp.toPx(), 12.dp.toPx()))
            else drawCircle(dot, 6.dp.toPx())
        }
    }
}

// C07 — Int delta chip + head-to-head counter with flash on the changed cell.
@Composable
fun IntDeltaChip(delta: Int, modifier: Modifier = Modifier) {
    val s = remember { Animatable(0f) }
    LaunchedEffect(delta) { delay(200); s.animateTo(1f, keyframes { durationMillis = PqMotion.Base; 1.2f at 150 }) }
    val good = delta >= 0
    Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value }.clip(PqCircle).background(if (good) PqColors.Progress else ChessMotion.Red).border(2.dp, PqColors.Ink, PqCircle).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text((if (good) "+" else "") + "$delta INT", style = PqType.Button.copy(color = PqColors.Paper))
    }
}

enum class H2HCell { Win, Draw, Loss }

@Composable
fun H2HCounter(wins: Int, draws: Int, losses: Int, changed: H2HCell?, opponent: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(pqShape(12.dp)).border(PqDimens.Border, PqColors.Ink, pqShape(12.dp))) {
        listOf(Triple(wins, "YOU", H2HCell.Win), Triple(draws, "DRAWS", H2HCell.Draw), Triple(losses, opponent.uppercase(), H2HCell.Loss)).forEachIndexed { i, (v, l, cell) ->
            val flash = remember { Animatable(0f) }
            LaunchedEffect(changed) { if (changed == cell) { delay(700); flash.snapTo(1f); flash.animateTo(0f, tween(500)) } }
            Box(Modifier.weight(1f).background(if (i == 0) PqColors.Gold else PqColors.Paper).drawBehind { drawRect(PqColors.Paper.copy(alpha = flash.value)) }.padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { RollingNumber(v, PqType.display(30)); Text(l, style = PqType.Label) }
                if (changed == cell) FloatingReward("+1", trigger = 1, modifier = Modifier.align(Alignment.TopEnd))
            }
            if (i < 2) Box(Modifier.width(2.dp).fillMaxHeight().background(PqColors.Ink))
        }
    }
}

// C08 — challenge clash + expiry bar.
@Composable
fun ChallengeClash(me: @Composable () -> Unit, them: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val k = remember { Animatable(0f) }; val vs = remember { Animatable(0f) }; val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        k.animateTo(1f, tween(PqMotion.Slow, easing = PqMotion.snap()))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { vs.animateTo(1f, keyframes { durationMillis = 260; 1.4f at 140 }) }
        shake.animateTo(0f, keyframes { durationMillis = 240; -4f at 40; 4f at 100; -2f at 170 })
    }
    Row(modifier.graphicsLayer { translationX = shake.value.dp.toPx() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.graphicsLayer { translationX = -130.dp.toPx() * (1f - k.value) }) { me() }
        RewardBurst(trigger = if (k.value >= 1f) 1 else 0, shards = 8) {
            Text("VS", style = PqType.display(36).copy(color = ChessMotion.Red), modifier = Modifier.graphicsLayer { scaleX = vs.value; scaleY = vs.value })
        }
        Box(Modifier.graphicsLayer { translationX = 130.dp.toPx() * (1f - k.value) }) { them() }
    }
}

@Composable
fun ExpiryBar(secondsLeft: Int, total: Int = 60, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(secondsLeft / total.toFloat(), tween(1000, easing = if (PqMotion.pixel) PqMotion.steps(2) else LinearEasing), label = "exp")
    Box(modifier.fillMaxWidth().height(12.dp).clip(PqCircle).background(PqColors.Sunken).border(2.dp, PqColors.Ink, PqCircle)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(ChessMotion.Red))
    }
}

// C09 — promotion: pawn shrinks out, flash, new piece spins in with rays.
@Composable
fun PromotionMorph(trigger: Int, square: Dp, pawn: @Composable () -> Unit, promoted: @Composable () -> Unit) {
    val out = remember(trigger) { Animatable(1f) }; val inn = remember(trigger) { Animatable(0f) }; val flash = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        launch { flash.snapTo(1f); flash.animateTo(0f, tween(300)) }
        out.animateTo(0f, tween(PqMotion.Fast))
        inn.animateTo(1f, keyframes { durationMillis = 300; 1.35f at 180 })
    }
    Box(Modifier.size(square).drawBehind { if (flash.value > 0f) drawRect(Color.White.copy(alpha = flash.value)) }, contentAlignment = Alignment.Center) {
        if (out.value > 0f) Box(Modifier.graphicsLayer { scaleX = out.value; scaleY = out.value; alpha = out.value }) { pawn() }
        RewardBurst(trigger = if (inn.value > 0f) trigger else 0, shards = 8) {
            Box(Modifier.graphicsLayer { scaleX = inn.value; scaleY = inn.value; rotationZ = (1f - inn.value) * -40f }) { promoted() }
        }
    }
}
