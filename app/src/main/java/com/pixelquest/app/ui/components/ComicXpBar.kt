package com.pixelquest.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
 * Step 11: ComicXpBar matching PixelXpBar role.
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
    energyColor: Color = ComicTokens.SkyBlue
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
        // Level Badge (Initial Step 11 layout, refined in Step 13)
        Box(
            modifier = Modifier
                .height(30.dp)
                .comicDropShadow(
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    color = ComicTokens.SolidBlack,
                    shape = RoundedCornerShape(6.dp)
                )
                .background(ComicTokens.CoralRed, RoundedCornerShape(6.dp))
                .comicBorder(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "LVL $level",
                fontFamily = BangersFontFamily,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                color = ComicTokens.SolidBlack
            )
        }

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
