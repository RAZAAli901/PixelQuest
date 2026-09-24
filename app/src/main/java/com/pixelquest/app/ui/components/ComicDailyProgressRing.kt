package com.pixelquest.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 8: ComicStarburstBadge — Action starburst shape for "Perfect Day!" celebratory moment.
 * Procedurally draws a multi-pointed comic action star with offset drop shadow and black ink border.
 */
@Composable
fun ComicStarburstBadge(
    text: String = "POW!",
    modifier: Modifier = Modifier,
    fillColor: Color = ComicTokens.GoldAccent,
    points: Int = 12
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val rOuter = minOf(w, h) / 2f * 0.95f
            val rInner = rOuter * 0.65f

            fun buildStarPath(offsetX: Float = 0f, offsetY: Float = 0f): Path {
                val path = Path()
                val totalPoints = points * 2
                val angleStep = (2.0 * Math.PI / totalPoints)

                for (i in 0 until totalPoints) {
                    val angle = i * angleStep - (Math.PI / 2.0)
                    val r = if (i % 2 == 0) rOuter else rInner
                    val x = cx + offsetX + (r * Math.cos(angle)).toFloat()
                    val y = cy + offsetY + (r * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                return path
            }

            // Drop shadow
            val shadowPx = 3.dp.toPx()
            drawPath(
                path = buildStarPath(shadowPx, shadowPx),
                color = ComicTokens.SolidBlack,
                style = Fill
            )

            // Fill
            drawPath(
                path = buildStarPath(),
                color = fillColor,
                style = Fill
            )

            // Ink Border
            drawPath(
                path = buildStarPath(),
                color = ComicTokens.SolidBlack,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Text(
            text = text,
            fontFamily = BangersFontFamily,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = ComicTokens.SolidBlack,
            modifier = Modifier.rotate(-6f)
        )
    }
}

/**
 * Step 6: Circular comic-styled progress ring featuring:
 * - Solid black outer contour and track outline
 * - Thick comic-colored arc fill with rounded caps
 * - Central high-contrast percentage display in Bangers typography
 */
@Composable
fun ComicProgressRingArc(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    strokeWidth: Dp = 8.dp,
    arcColor: Color = ComicTokens.CoralRed,
    trackColor: Color = ComicTokens.PanelSurface,
    animate: Boolean = true
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (animate) tween(durationMillis = 500, easing = FastOutSlowInEasing) else tween(0),
        label = "comic_ring_arc"
    )

    val sweepAngle = 360f * animatedProgress
    val pctInt = (clamped * 100).toInt()

    Box(
        modifier = modifier
            .size(size)
            .comicDropShadow(
                offsetX = ComicShapeTokens.ShadowOffsetSmall,
                offsetY = ComicShapeTokens.ShadowOffsetSmall,
                color = ComicTokens.SolidBlack,
                shape = CircleShape
            )
            .background(color = trackColor, shape = CircleShape)
            .comicBorder(
                width = ComicShapeTokens.BorderWidthDefault,
                color = ComicTokens.SolidBlack,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 8.dp)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Track background ring
            drawArc(
                color = ComicTokens.SurfaceVariant,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = "$pctInt%",
            fontFamily = BangersFontFamily,
            fontSize = 17.sp,
            letterSpacing = 0.5.sp,
            color = ComicTokens.SolidBlack
        )
    }
}

/**
 * Step 6 & 8: ComicDailyProgressRing composable matching PixelDailyProgressRing's role.
 * Wraps the circular comic ring inside a comic panel with title, target status, and bold typography.
 * Displays ComicStarburstBadge during "Perfect Day!" completion.
 */
@Composable
fun ComicDailyProgressRing(
    progress: Float,
    targetThreshold: Float,
    modifier: Modifier = Modifier
) {
    val pctInt = (progress * 100).toInt()
    val targetPctInt = (targetThreshold * 100).toInt()
    val isGoalMet = progress >= targetThreshold

    val arcColor = if (isGoalMet) ComicTokens.GoldAccent else ComicTokens.CoralRed
    val panelVariant = if (isGoalMet) ComicPanelVariant.BURNT_ORANGE else ComicPanelVariant.SURFACE

    ComicPanel(
        variant = panelVariant,
        contentPadding = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isGoalMet) {
                        ComicStarburstBadge(
                            text = "POW!",
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isGoalMet) "PERFECT DAY!" else "QUEST PROGRESS",
                        fontFamily = BangersFontFamily,
                        fontSize = 18.sp,
                        letterSpacing = 0.8.sp,
                        color = ComicTokens.SolidBlack
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isGoalMet) "ALL DAILY GOALS CRUSHED!" else "DAILY HABIT COMPLETION",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComicTokens.SolidBlack.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TARGET: $pctInt% / $targetPctInt%",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = ComicTokens.SolidBlack
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            ComicProgressRingArc(
                progress = progress,
                size = 72.dp,
                arcColor = arcColor
            )
        }
    }
}

