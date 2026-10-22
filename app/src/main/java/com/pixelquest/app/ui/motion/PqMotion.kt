package com.pixelquest.app.ui.motion

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.SystemMotion
import com.pixelquest.app.ui.theme.pqShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * PixelQuest micro-animations, from the owner's animations handoff (design-reference/PixelQuest
 * Animations.dc.html, M01 to M12). Comic is springy overshoot; Pixel is hard steps(4). Everything
 * reads PqMotion, so switching the skin switches the motion too. With motion reduced (the app's
 * Reduce motion, or Android's Remove animations), each one jumps to its end state.
 */

// ============================ TOKENS ============================
object PqMotion {
    const val Fast = 120
    const val Base = 240
    const val Slow = 420
    const val Celebrate = 900

    val Snap: Easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.3f)
    val Out: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    fun steps(n: Int) = Easing { x -> if (x >= 1f) 1f else floor(x * n) / n }

    val pixel: Boolean get() = PqSkinState.skin == PqSkin.Pixel
    fun snap(): Easing = if (pixel) steps(4) else Snap
    fun out(): Easing = if (pixel) steps(4) else Out
}

/** True when motion is reduced: the app's Reduce motion switch, or Android's Remove animations. */
@Composable
fun reducedMotion(): Boolean {
    val app = LocalReduceMotion.current
    val ctx = LocalContext.current
    val system = remember { SystemMotion.animationsOff(ctx.contentResolver) }
    return app || system
}

// ============================ M01 / M02  XP BAR ============================
/**
 * Animated XP bar. Pass the current values; it animates from the ones it last showed. When [level]
 * goes up: fill to 100%, flash gold twice, snap to 0, then fill what's left over (M02).
 */
@Composable
fun XpBar(xp: Int, max: Int, level: Int, modifier: Modifier = Modifier, height: Dp = 18.dp, onLevelUpShown: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val safeMax = max.coerceAtLeast(1)
    val fill = remember { Animatable(xp / safeMax.toFloat()) }
    val flash = remember { Animatable(0f) }
    val shine = remember { Animatable(-0.2f) }
    var lastLevel by remember { mutableIntStateOf(level) }
    LaunchedEffect(xp, safeMax, level) {
        val target = (xp / safeMax.toFloat()).coerceIn(0f, 1f)
        if (reduced) { fill.snapTo(target); lastLevel = level; return@LaunchedEffect }
        if (fill.value == target && level == lastLevel) return@LaunchedEffect
        if (level > lastLevel) {
            fill.animateTo(1f, tween(PqMotion.Slow, easing = PqMotion.out()))
            repeat(2) { flash.animateTo(1f, tween(80)); flash.animateTo(0f, tween(80)) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            fill.snapTo(0f)
            lastLevel = level
            onLevelUpShown()
        }
        fill.animateTo(target, tween(600, easing = PqMotion.snap()))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (!PqMotion.pixel) { shine.snapTo(-0.2f); shine.animateTo(1.2f, tween(PqMotion.Slow, easing = LinearEasing)) }
    }
    val shape = pqShape(999.dp)
    Box(
        modifier.fillMaxWidth().height(height).clip(shape).background(PqColors.Sunken).border(PqDimens.Border, PqColors.Ink, shape)
            .drawWithContent {
                drawContent()
                val w = size.width * fill.value.coerceIn(0f, 1.05f)
                drawRect(PqColors.Progress, size = Size(w, size.height))
                if (shine.value in 0f..1f) {
                    val x = w * shine.value
                    drawRect(Color.White.copy(alpha = 0.55f), topLeft = Offset(x - 10.dp.toPx(), 0f), size = Size(14.dp.toPx(), size.height))
                }
                if (flash.value > 0f) drawRect(PqColors.Gold.copy(alpha = flash.value))
            },
    )
}

/** Rolling number (M01, M05, M07): the old value slides out and the new one slides in. Never jumps. */
@Composable
fun RollingNumber(value: Int, style: TextStyle, modifier: Modifier = Modifier, format: (Int) -> String = { "%,d".format(it) }) {
    val reduced = reducedMotion()
    AnimatedContent(
        targetState = value, modifier = modifier, label = "roll",
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                val up = targetState > initialState
                (slideInVertically(tween(PqMotion.Base, easing = PqMotion.out())) { if (up) it else -it } + fadeIn(tween(PqMotion.Fast))) togetherWith
                    (slideOutVertically(tween(PqMotion.Base, easing = PqMotion.out())) { if (up) -it else it } + fadeOut(tween(PqMotion.Fast)))
            }
        },
    ) { Text(format(it), style = style) }
}

/** "+20 XP" chip that pops, then flies by [flyTo] and fades. Increment [trigger] to replay. */
@Composable
fun XpGainChip(amount: Int, trigger: Int, modifier: Modifier = Modifier, flyTo: IntOffset = IntOffset(64, 58)) {
    if (trigger == 0 || reducedMotion()) return
    val scale = remember(trigger) { Animatable(0f) }
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        scale.animateTo(1f, keyframes { durationMillis = 200; 1.2f at 120 })
        t.animateTo(1f, tween(500, easing = PqMotion.out()))
    }
    val d = LocalDensity.current
    if (t.value >= 1f) return
    Box(
        modifier
            .graphicsLayer {
                val k = t.value
                translationX = with(d) { flyTo.x.dp.toPx() } * k
                translationY = with(d) { flyTo.y.dp.toPx() } * k
                val s = scale.value * (1f - 0.6f * k); scaleX = s; scaleY = s
                alpha = if (k > 0.9f) (1f - k) * 10f else 1f
            }
            .clip(PqCircle).background(PqColors.Gold).border(2.dp, PqColors.Ink, PqCircle).padding(horizontal = 9.dp, vertical = 5.dp),
    ) { Text(stringResource(R.string.motion_xp_gain, amount), style = PqType.Label) }
}

/** The level as a gold badge that pops and rolls when it goes up (M02). */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(1f) }
    var prev by remember { mutableIntStateOf(level) }
    val reduced = reducedMotion()
    LaunchedEffect(level) {
        if (level > prev && !reduced) pop.animateTo(1f, keyframes { durationMillis = 360; 1.35f at 120; 0.95f at 240 })
        prev = level
    }
    val label = stringResource(R.string.motion_level)
    Box(modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = (pop.value - 1f) * -18f }.comic(PqColors.Gold, 12.dp).padding(horizontal = 10.dp, vertical = 4.dp)) {
        RollingNumber(level, PqType.display(30), format = { "$label $it" })
    }
}

// ============================ M03  QUEST CHECK ============================
/** The quest's round check: fills with a pop and draws its tick (M03). */
@Composable
fun QuestCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, size: Dp = 38.dp) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val fill = remember { Animatable(if (checked) 1f else 0f) }
    val draw = remember { Animatable(if (checked) 1f else 0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(checked) {
        if (first) { first = false; return@LaunchedEffect }
        when {
            reduced -> { fill.snapTo(if (checked) 1f else 0f); draw.snapTo(if (checked) 1f else 0f) }
            checked -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                fill.animateTo(1f, keyframes { durationMillis = PqMotion.Base; 1.15f at 160 })
                draw.animateTo(1f, tween(200, easing = PqMotion.out()))
            }
            else -> { draw.snapTo(0f); fill.animateTo(0f, tween(PqMotion.Fast)) }
        }
    }
    val ink = PqColors.Ink
    val paper = PqColors.Paper
    val dot = if (PqMotion.pixel) PqColors.Progress else ink
    Box(modifier.size(size).clip(PqCircle).background(paper).border(PqDimens.Border, ink, PqCircle).clickableNoRipple(onToggle)) {
        QuestTick(fill.value, draw.value, dot, paper)
    }
}

/** The check's fill and tick at [fill] and [draw] (0 to 1), for QuestCheck and for rows that draw their own. */
@Composable
fun QuestTick(fill: Float, draw: Float, dot: Color, tick: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val r = (this.size.minDimension / 2 - 2.dp.toPx()) * fill
        if (PqMotion.pixel) drawRect(dot, topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2)) else drawCircle(dot, r)
        if (draw > 0f) {
            val p = Path().apply {
                moveTo(this@Canvas.size.width * 0.27f, this@Canvas.size.height * 0.52f)
                lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.69f)
                lineTo(this@Canvas.size.width * 0.75f, this@Canvas.size.height * 0.33f)
            }
            val pm = PathMeasure().apply { setPath(p, false) }
            val seg = Path()
            pm.getSegment(0f, pm.length * draw, seg, true)
            drawPath(
                seg, tick,
                style = Stroke(3.2.dp.toPx(), cap = if (PqMotion.pixel) StrokeCap.Square else StrokeCap.Round, join = if (PqMotion.pixel) StrokeJoin.Miter else StrokeJoin.Round)
            )
        }
    }
}

/** On a quest row: a squash when it's done, then dimmed to half ([dim]; rows with their own done look skip it). */
fun Modifier.questRowDone(done: Boolean, dim: Boolean = true): Modifier = composed {
    val reduced = reducedMotion()
    val squash = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(done) {
        if (first) { first = false; return@LaunchedEffect }
        if (done && !reduced) squash.animateTo(1f, keyframes { durationMillis = 300; 0.97f at 80; 1.02f at 200 })
    }
    val a by animateFloatAsState(if (done && dim) 0.5f else 1f, if (reduced) snap() else tween(PqMotion.Base), label = "dim")
    graphicsLayer { scaleX = squash.value; scaleY = squash.value; alpha = a }
}

/** How far a done quest's title is struck through, 0 to 1. */
@Composable
fun strikeProgress(done: Boolean): Float {
    val reduced = reducedMotion()
    return animateFloatAsState(
        if (done) 1f else 0f,
        if (reduced) snap() else tween(PqMotion.Base, delayMillis = 60, easing = PqMotion.out()),
        label = "strike"
    ).value
}

/** Draws a strike through the middle of what it's on, [progress] of the way across. */
fun Modifier.strikeThrough(progress: Float, color: Color = PqColors.Ink): Modifier = drawWithContent {
    drawContent()
    if (progress > 0f) {
        val y = size.height / 2
        drawLine(color, Offset(0f, y), Offset(size.width * progress, y), strokeWidth = 2.dp.toPx())
    }
}

/** "+20 XP" floating up and fading from where it's placed. */
@Composable
fun FloatingReward(text: String, trigger: Int, modifier: Modifier = Modifier, color: Color = Color(0xFF1F7A3A)) {
    if (trigger == 0) return
    val reduced = reducedMotion()
    val t = remember(trigger) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(trigger) { if (!reduced) t.animateTo(1f, tween(900, easing = LinearEasing)) }
    if (reduced) return
    Text(text, style = PqType.Label.copy(color = color), modifier = modifier.graphicsLayer {
        translationY = -36.dp.toPx() * t.value
        alpha = when {
            t.value < 0.15f -> t.value / 0.15f
            t.value > 0.8f -> (1f - t.value) / 0.2f
            else -> 1f
        }
    })
}

// ============================ M04  PRESS ============================
/** Press motion for comic and pixel surfaces: down with the press, a spring back on release. */
fun Modifier.pqPress(interaction: MutableInteractionSource, shadow: Dp = PqDimens.Shadow): Modifier = composed {
    val reduced = reducedMotion()
    val pressed by interaction.collectIsPressedAsState()
    val off by animateDpAsState(
        if (pressed) shadow else 0.dp,
        when {
            reduced -> snap()
            pressed -> tween(PqMotion.Fast, easing = PqMotion.out())
            else -> tween(PqMotion.Base, easing = PqMotion.snap())
        },
        label = "press"
    )
    offset(x = if (PqMotion.pixel) 0.dp else off, y = off)
}

// ============================ M05  STREAK ============================
/** The streak chip: a flickering flame, and a pop with a rolling number when it grows. */
@Composable
fun StreakCounter(days: Int, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(1f) }
    var prev by remember { mutableIntStateOf(days) }
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    LaunchedEffect(days) {
        if (days > prev && !reduced) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pop.animateTo(1f, keyframes { durationMillis = 400; 1.25f at 140; 0.95f at 280 })
        }
        prev = days
    }
    Row(
        modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = (pop.value - 1f) * 16f }
            .comic(PqColors.Gold, 999.dp).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Flame(reduced, Modifier.size(24.dp))
        RollingNumber(days, PqType.Button)
    }
}

/** The streak's flame, flickering unless motion is reduced. */
@Composable
fun Flame(still: Boolean, modifier: Modifier = Modifier, tint: Color = PqColors.Flame) {
    if (still) {
        Icon(Icons.Rounded.LocalFireDepartment, null, tint = tint, modifier = modifier)
        return
    }
    val inf = rememberInfiniteTransition(label = "flame")
    val f by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = if (PqMotion.pixel) PqMotion.steps(3) else LinearEasing)), label = "f")
    Icon(Icons.Rounded.LocalFireDepartment, null, tint = tint, modifier = modifier.graphicsLayer {
        scaleX = 1f - 0.06f * sin(f * 6.283f)
        scaleY = 1f + 0.1f * sin(f * 6.283f)
        transformOrigin = TransformOrigin(0.5f, 0.9f)
    })
}

// ============================ M06  BURST ============================
/** A radial burst of shards and a pop around [content]. Increment [trigger] to fire. */
@Composable
fun RewardBurst(trigger: Int, modifier: Modifier = Modifier, shards: Int = 10, content: @Composable () -> Unit) {
    val reduced = reducedMotion()
    val idle = trigger == 0 || reduced
    val t = remember(trigger) { Animatable(if (idle) 1f else 0f) }
    val pop = remember(trigger) { Animatable(if (idle) 1f else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(trigger) {
        if (idle) return@LaunchedEffect
        scope.launch { pop.animateTo(1f, keyframes { durationMillis = 400; 1.35f at 200 }) }
        t.animateTo(1f, tween(PqMotion.Celebrate, easing = PqMotion.out()))
    }
    val colors = listOf(PqColors.Gold, PqColors.Red, PqColors.Sky, PqColors.Lilac)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            if (t.value >= 1f) return@Canvas
            val d = 18.dp.toPx() + 46.dp.toPx() * t.value
            val s = 8.dp.toPx() * (1.2f - 0.9f * t.value)
            for (i in 0 until shards) {
                val a = (i * 2 * Math.PI / shards).toFloat()
                val c = Offset(center.x + cos(a) * d, center.y + sin(a) * d)
                drawRect(PqColors.Ink.copy(alpha = 1f - t.value), topLeft = c - Offset(s / 2 + 1.5f, s / 2 + 1.5f), size = Size(s + 3f, s + 3f))
                drawRect(colors[i % colors.size].copy(alpha = 1f - t.value), topLeft = c - Offset(s / 2, s / 2), size = Size(s, s))
            }
        }
        Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = (1f - pop.value) * -30f }) { content() }
    }
}

// ============================ M07  PROGRESS ============================
/** The day's progress as a ring that sweeps to its value (Comic). */
@Composable
fun ProgressRingAnimated(progress: Float, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val reduced = reducedMotion()
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), if (reduced) snap() else tween(700, easing = PqMotion.out()), label = "ring")
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.1f
        val r = this.size.minDimension / 2 - stroke
        drawCircle(PqColors.Ink, r, style = Stroke(stroke * 1.4f))
        drawCircle(PqColors.Sunken, r, style = Stroke(stroke))
        rotate(-90f) { drawArc(PqColors.Progress, 0f, 360f * p, false, topLeft = center - Offset(r, r), size = Size(r * 2, r * 2), style = Stroke(stroke)) }
    }
}

/** The Pixel skin's ring: blocks that fill one by one, 160 ms apart. */
@Composable
fun PixelMeter(progress: Float, modifier: Modifier = Modifier, blocks: Int = 10) {
    val target = (progress.coerceIn(0f, 1f) * blocks).toInt()
    val reduced = reducedMotion()
    var shown by remember { mutableIntStateOf(if (reduced) target else 0) }
    LaunchedEffect(target) {
        if (reduced) { shown = target; return@LaunchedEffect }
        while (shown != target) { shown += if (target > shown) 1 else -1; delay(160) }
    }
    Row(modifier.border(2.dp, PqColors.Ink).background(PqColors.Paper).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(blocks) { i ->
            val on = i < shown
            val s by animateFloatAsState(if (on) 1f else 0.85f, tween(PqMotion.Fast, easing = PqMotion.steps(2)), label = "b$i")
            Box(Modifier.weight(1f).height(22.dp).graphicsLayer { scaleX = s; scaleY = s }.background(if (on) PqColors.Progress else PqColors.Sunken).border(2.dp, PqColors.Ink))
        }
    }
}

// ============================ M08  PERFECT DAY ============================
/** "PERFECT!" stamped onto the Perfect Day card. */
@Composable
fun PerfectDayStamp(visible: Boolean, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val reduced = reducedMotion()
    val s = remember { Animatable(if (visible) 1f else 2.6f) }
    val a = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        when {
            !visible -> { a.snapTo(0f); s.snapTo(2.6f) }
            reduced -> { a.snapTo(1f); s.snapTo(1f) }
            s.value != 1f -> {
                launch { a.animateTo(1f, tween(PqMotion.Fast)) }
                s.animateTo(1f, keyframes { durationMillis = 360; 0.9f at 280 })
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    val red = if (PqMotion.pixel) Color(0xFFC25E00) else Color(0xFFD63A2F)
    Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; rotationZ = -12f; alpha = a.value }
        .border(3.dp, red, pqShape(8.dp)).background(PqColors.Paper.copy(alpha = 0.85f), pqShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(stringResource(R.string.motion_perfect), style = PqType.display(30).copy(color = red))
    }
}

/** Shakes the card the stamp lands on. */
fun Modifier.thud(trigger: Int): Modifier = composed {
    val reduced = reducedMotion()
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0 && !reduced) { delay(280); x.animateTo(0f, keyframes { durationMillis = 240; -4f at 40; 4f at 100; -2f at 170 }) }
    }
    graphicsLayer { translationX = x.value.dp.toPx() }
}

/** Confetti falling over the screen. Increment [trigger] to drop it again. */
@Composable
fun ConfettiFall(trigger: Int, modifier: Modifier = Modifier, pieces: Int = 18) {
    if (trigger == 0 || reducedMotion()) return
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.animateTo(1f, tween(2000, easing = LinearEasing)) }
    val seeds = remember(trigger) {
        val r = java.util.Random(trigger.toLong())
        List(pieces) { floatArrayOf(r.nextFloat(), r.nextFloat() * 2 - 1, r.nextFloat() * 360, r.nextFloat() * 0.3f) }
    }
    val colors = listOf(PqColors.Red, PqColors.Sky, PqColors.Lilac, PqColors.Gold, PqColors.Ink)
    if (t.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        seeds.forEachIndexed { i, seed ->
            val x = seed[0]
            val dx = seed[1]
            val rot = seed[2]
            val d = seed[3]
            val k = ((t.value - d) / (1f - d)).coerceIn(0f, 1f)
            if (k <= 0f || k >= 1f) return@forEachIndexed
            val p = Offset(size.width * x + dx * 40.dp.toPx() * k, -20f + size.height * 1.1f * k)
            rotate(rot * k, p) {
                drawRect(
                    colors[i % colors.size].copy(alpha = if (k > 0.85f) (1 - k) / 0.15f else 1f), topLeft = p,
                    size = Size(8.dp.toPx(), if (PqMotion.pixel) 8.dp.toPx() else 12.dp.toPx())
                )
            }
        }
    }
}

// ============================ M09  TOAST ============================
/** What the toast shows: a message and, for an undoable action, its button. */
class PqToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var action by mutableStateOf<String?>(null)
        private set
    internal var onAction: (() -> Unit)? = null
    internal var id by mutableIntStateOf(0)

    fun show(message: String, action: String? = null, onAction: (() -> Unit)? = null) {
        this.message = message
        this.action = action
        this.onAction = onAction
        id++
    }
}

/** The app's one toast state, provided by the root scaffold. Null where there's no host. */
val LocalPqToast = staticCompositionLocalOf<PqToastState?> { null }

/** The toast: slides up with an overshoot, stays four seconds, and goes. */
@Composable
fun PqToastHost(state: PqToastState, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val reduced = reducedMotion()
    LaunchedEffect(state.id) {
        if (state.id != 0) {
            visible = true
            delay(4000)
            visible = false
        }
    }
    val enterEase = if (PqMotion.pixel) PqMotion.steps(4) else CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)
    AnimatedVisibility(
        visible, modifier,
        enter = if (reduced) fadeIn(snap()) else slideInVertically(tween(PqMotion.Slow, easing = enterEase)) { it * 2 },
        exit = if (reduced) fadeOut(snap()) else slideOutVertically(tween(PqMotion.Base)) { it * 2 }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp).height(50.dp).clip(pqShape(12.dp)).background(PqColors.Ink).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, null, tint = if (PqMotion.pixel) PqColors.Progress else PqColors.Gold)
            Text(state.message.orEmpty(), style = PqType.BodyStrong.copy(color = PqColors.Paper), modifier = Modifier.weight(1f))
            state.action?.let { a ->
                Text(
                    a.uppercase(), style = PqType.Button.copy(color = PqColors.Gold),
                    modifier = Modifier.clickableNoRipple { state.onAction?.invoke(); visible = false }.padding(10.dp)
                )
            }
        }
    }
}

// ============================ M10  NAV ============================
/** The tab icon's hop when it's selected. */
fun Modifier.navHop(selected: Boolean): Modifier = composed {
    val reduced = reducedMotion()
    val y = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (first) { first = false; return@LaunchedEffect }
        if (selected && !reduced) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            y.animateTo(0f, keyframes { durationMillis = 300; -8f at 110 })
        }
    }
    graphicsLayer { translationY = y.value.dp.toPx(); val s = 1f + (-y.value / 8f) * 0.15f; scaleX = s; scaleY = s }
}

/** A gold indicator that slides under the selected tab. Put it in a Box behind the tabs' Row. */
@Composable
fun NavIndicator(index: Int, tabWidth: Dp, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val ease = if (PqMotion.pixel) PqMotion.steps(4) else CubicBezierEasing(0.3f, 1.3f, 0.5f, 1f)
    val x by animateDpAsState(tabWidth * index, if (reduced) snap() else tween(PqMotion.Slow, easing = ease), label = "ind")
    Box(modifier.offset(x = x).width(tabWidth).fillMaxHeight().padding(6.dp).clip(pqShape(12.dp)).background(PqColors.Gold).border(2.dp, PqColors.Ink, pqShape(12.dp)))
}

// ============================ M11  PRAYER PIPS ============================
/** Today's five prayers as pips that pop in as each is marked. */
@Composable
fun PrayerPips(done: List<Boolean>, names: List<String>, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { i, n ->
            val on = done.getOrElse(i) { false }
            val s by animateFloatAsState(
                if (on) 1f else 0f,
                when {
                    reduced -> snap()
                    on -> keyframes { durationMillis = PqMotion.Base; 1.25f at 140 }
                    else -> tween(PqMotion.Fast)
                },
                label = "pip$i"
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().height(36.dp).dashedBorder(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.matchParentSize().graphicsLayer { scaleX = s; scaleY = s }.clip(pqShape(9.dp)).background(PqColors.Paper).border(2.dp, PqColors.Ink, pqShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (s > 0.01f) Icon(Icons.Rounded.Check, null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                    }
                }
                Text(n, style = PqType.Meta.copy(color = PqColors.Ink), maxLines = 1)
            }
        }
    }
}

// ============================ M12  SHIMMER ============================
/** A skeleton block's shimmer, for lists that take more than 250 ms to load. */
fun Modifier.pqShimmer(): Modifier = composed {
    val reduced = reducedMotion()
    val base = PqColors.Sunken
    val hi = PqColors.Paper
    if (reduced) return@composed background(base)
    val inf = rememberInfiniteTransition(label = "shimmer")
    val x by inf.animateFloat(-1f, 2f, infiniteRepeatable(tween(1400, easing = if (PqMotion.pixel) PqMotion.steps(8) else LinearEasing), RepeatMode.Restart), label = "x")
    drawWithContent {
        val w = size.width
        drawRect(Brush.linearGradient(listOf(base, base, hi, base, base), start = Offset(w * x - w, 0f), end = Offset(w * x + w, 0f)))
    }
}

/**
 * Skeleton rows that shimmer while a list loads. They appear only after 250 ms, so a quick load
 * never flashes them (M12). TalkBack hears "Loading".
 */
@Composable
fun PqSkeleton(modifier: Modifier = Modifier, rows: Int = 4, rowHeight: Dp = 64.dp) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(250); show = true }
    if (!show) return
    val loading = stringResource(R.string.motion_loading)
    Column(
        modifier.fillMaxWidth().testTag(PqSkeletonTag).semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) {
            Box(
                Modifier.fillMaxWidth().height(rowHeight).clip(pqShape(PqDimens.Radius))
                    .border(PqDimens.BorderThin, PqColors.Ink.copy(alpha = 0.2f), pqShape(PqDimens.Radius)).pqShimmer()
            )
        }
    }
}

const val PqSkeletonTag = "pq_skeleton"

// ============================ helpers ============================
private fun Modifier.clickableNoRipple(onClick: () -> Unit) = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

private fun Modifier.dashedBorder() = drawWithContent {
    drawContent()
    drawRoundRect(
        PqColors.Ink, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
        cornerRadius = CornerRadius(if (PqMotion.pixel) 0f else 9.dp.toPx())
    )
}
