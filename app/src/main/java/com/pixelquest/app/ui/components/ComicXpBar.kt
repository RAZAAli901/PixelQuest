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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Step 11: Comic-styled diagonal-striped energy fill canvas.
 * Draws vibrant comic energy fill with diagonal action stripes.
 */
@Composable
fun ComicEnergyFill(
    fraction: Float,
    modifier: Modifier = Modifier,
    baseColor: Color = ComicTokens.SkyBlue,
    stripeColor: Color = Color.White.copy(alpha = 0.35f),
    stripeWidthDp: Dp = 6.dp,
    stripeSpacingDp: Dp = 12.dp
) {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    if (clampedFraction <= 0f) return

    val fillShape = RoundedCornerShape(5.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(clampedFraction)
            .clip(fillShape)
            .background(baseColor)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val stripeWidthPx = stripeWidthDp.toPx()
            val stripeSpacingPx = stripeSpacingDp.toPx()
            val totalSpacing = stripeWidthPx + stripeSpacingPx

            var startX = -h
            while (startX < w + h) {
                drawLine(
                    color = stripeColor,
                    start = Offset(startX, h),
                    end = Offset(startX + h, 0f),
                    strokeWidth = stripeWidthPx
                )
                startX += totalSpacing
            }
        }
    }
}

/**
 * Step 13: Comic level badge style options.
 */
enum class ComicLevelBadgeStyle {
    CIRCLE,
    BURST
}

/**
 * Step 13: Comic-styled level badge alongside the XP bar.
 * Features a circular or starburst action badge with thick black ink border,
 * comic drop shadow, and Bangers typography.
 */
@Composable
fun ComicLevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
    style: ComicLevelBadgeStyle = ComicLevelBadgeStyle.CIRCLE,
    backgroundColor: Color = ComicTokens.GoldAccent
) {
    val sizeDp = 38.dp
    if (style == ComicLevelBadgeStyle.BURST) {
        Box(
            modifier = modifier
                .size(sizeDp)
                .semantics { contentDescription = "Level $level badge" },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.minDimension / 2f - 3f
                val innerR = outerR * 0.72f
                val points = 10
                val angleStep = (2f * Math.PI / points).toFloat()
                val halfStep = angleStep / 2f

                val path = Path()
                for (i in 0 until points) {
                    val outerAngle = i * angleStep - (Math.PI / 2f).toFloat()
                    val innerAngle = outerAngle + halfStep
                    val ox = cx + outerR * cos(outerAngle)
                    val oy = cy + outerR * sin(outerAngle)
                    val ix = cx + innerR * cos(innerAngle)
                    val iy = cy + innerR * sin(innerAngle)

                    if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
                    path.lineTo(ix, iy)
                }
                path.close()

                // Shadow
                drawContext.canvas.save()
                drawContext.canvas.translate(2f.toDp().toPx(), 2f.toDp().toPx())
                drawPath(path, color = ComicTokens.SolidBlack)
                drawContext.canvas.restore()

                // Background
                drawPath(path, color = backgroundColor)

                // Ink outline
                drawPath(path, color = ComicTokens.SolidBlack, style = Stroke(width = 2.5f.toDp().toPx()))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "LVL",
                    fontFamily = BangersFontFamily,
                    fontSize = 8.sp,
                    color = ComicTokens.SolidBlack,
                    lineHeight = 8.sp
                )
                Text(
                    text = "$level",
                    fontFamily = BangersFontFamily,
                    fontSize = 15.sp,
                    color = ComicTokens.SolidBlack,
                    lineHeight = 15.sp
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(sizeDp)
                .comicDropShadow(
                    offsetX = ComicShapeTokens.ShadowOffsetSmall,
                    offsetY = ComicShapeTokens.ShadowOffsetSmall,
                    color = ComicTokens.SolidBlack,
                    shape = CircleShape
                )
                .background(backgroundColor, CircleShape)
                .comicBorder(
                    width = ComicShapeTokens.BorderWidthDefault,
                    color = ComicTokens.SolidBlack,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "LVL",
                    fontFamily = BangersFontFamily,
                    fontSize = 8.sp,
                    color = ComicTokens.SolidBlack,
                    lineHeight = 8.sp
                )
                Text(
                    text = "$level",
                    fontFamily = BangersFontFamily,
                    fontSize = 15.sp,
                    color = ComicTokens.SolidBlack,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Step 11 & 13: ComicXpBar matching PixelXpBar role.
 * Features:
 * - Comic level badge on the left
 * - Comic panel track with flat black drop shadow and black ink border
 * - Diagonal-striped dynamic energy fill
 * - High-contrast progress text
 */
@Composable
fun ComicXpBar(
    currentProgress: Int,
    maxProgress: Int,
    modifier: Modifier = Modifier,
    level: Int = 1,
    energyColor: Color = ComicTokens.SkyBlue,
    badgeStyle: ComicLevelBadgeStyle = ComicLevelBadgeStyle.CIRCLE
) {
    val targetFraction = if (maxProgress > 0) (currentProgress.toFloat() / maxProgress).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "comic_xp_anim"
    )

    val percentage = (targetFraction * 100).toInt()
    val shadowOffset = ComicShapeTokens.ShadowOffsetSmall // 3.dp
    val trackShape = RoundedCornerShape(ComicShapeTokens.RadiusSmall) // 8.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Level $level XP progress: $currentProgress of $maxProgress days ($percentage percent)"
            }
    ) {
        // Step 13: Comic Level Badge alongside the bar
        ComicLevelBadge(
            level = level,
            style = badgeStyle
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Progress Track Box with Drop Shadow & Border
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp + shadowOffset)
                .padding(end = shadowOffset, bottom = shadowOffset),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .comicDropShadow(
                        offsetX = shadowOffset,
                        offsetY = shadowOffset,
                        color = ComicTokens.SolidBlack,
                        shape = trackShape
                    )
                    .background(ComicTokens.PanelSurface, shape = trackShape)
                    .comicBorder(
                        width = ComicShapeTokens.BorderWidthDefault,
                        color = ComicTokens.SolidBlack,
                        shape = trackShape
                    )
                    .padding(2.5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Diagonal-striped energy fill
                ComicEnergyFill(
                    fraction = animatedFraction,
                    baseColor = energyColor
                )

                // High-contrast text label
                Text(
                    text = "$currentProgress / $maxProgress DAYS",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = ComicTokens.SolidBlack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
