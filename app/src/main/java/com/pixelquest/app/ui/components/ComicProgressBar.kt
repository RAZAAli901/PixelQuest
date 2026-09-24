package com.pixelquest.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 1: ComicProgressBar matching PixelProgressBar's role and behavior.
 * Built with Compose-drawn vector geometry:
 * - Solid black ink border (2.5dp)
 * - Flat unblurred drop shadow (3dp offset)
 * - Crisp comic panel surface track
 * - Saturated pop-art fill (defaults to Coral Red) with smooth animated fill transitions
 */
@Composable
fun ComicProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    fillColor: Color = ComicTokens.CoralRed,
    trackColor: Color = ComicTokens.PanelSurface,
    animateTransition: Boolean = true
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = if (animateTransition) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "comic_progress_anim"
    )

    val effectiveProgress = if (animateTransition) animatedProgress else clampedProgress
    val shadowOffset: Dp = ComicShapeTokens.ShadowOffsetSmall // 3.dp
    val trackShape = RoundedCornerShape(ComicShapeTokens.RadiusSmall) // 8.dp
    val fillShape = RoundedCornerShape(6.dp)

    // Outer bounding Box reserves shadow clearance so layout never clips
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + shadowOffset)
            .padding(end = shadowOffset, bottom = shadowOffset),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Box with flat drop shadow, solid surface fill, and ink border
        Box(
            modifier = Modifier
                .matchParentSize()
                .comicDropShadow(
                    offsetX = shadowOffset,
                    offsetY = shadowOffset,
                    color = ComicTokens.SolidBlack,
                    shape = trackShape
                )
                .background(color = trackColor, shape = trackShape)
                .comicBorder(
                    width = ComicShapeTokens.BorderWidthDefault,
                    color = ComicTokens.SolidBlack,
                    shape = trackShape
                )
                .padding(2.5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Fill Indicator
            if (effectiveProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(effectiveProgress)
                        .background(color = fillColor, shape = fillShape)
                )
            }
        }
    }
}
