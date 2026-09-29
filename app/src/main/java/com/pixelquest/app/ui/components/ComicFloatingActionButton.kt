package com.pixelquest.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComicTypography
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 5 (Day 23): Comic-styled Floating Action Button (FAB).
 *
 * Implements ComicButton's signature styling & press physics:
 * - Solid 2.5dp black ink border ([ComicShapeTokens.BorderWidthDefault]).
 * - Hard-edged flat solid black drop shadow (4dp offset at rest).
 * - Vibrant Coral Red fill ([ComicTokens.CoralRed]).
 * - Tactile press actuation: face shifts +3dp into shadow while shadow collapses to 1dp.
 * - Bangers font "+" glyph or custom composable content with click sound & haptics.
 */
@Composable
fun ComicFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(ComicShapeTokens.RadiusDefault),
    backgroundColor: Color = ComicTokens.CoralRed,
    contentColor: Color = ComicTokens.SolidBlack,
    content: @Composable () -> Unit = {
        Text(
            text = "+",
            style = ComicTypography.headlineMedium,
            fontSize = 32.sp,
            color = contentColor
        )
    }
) {
    val soundManager = LocalSoundManager.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val maxShadow = ComicButtonPhysics.MaxShadow // 4.dp
    val minShadow = ComicButtonPhysics.MinShadow // 1.dp

    val currentShadow by animateDpAsState(
        targetValue = if (isPressed) minShadow else maxShadow,
        animationSpec = tween(durationMillis = ComicButtonPhysics.AnimationDurationMs),
        label = "comicFabShadow"
    )

    val currentTranslation by animateDpAsState(
        targetValue = if (isPressed) ComicButtonPhysics.TranslationDelta else 0.dp,
        animationSpec = tween(durationMillis = ComicButtonPhysics.AnimationDurationMs),
        label = "comicFabTranslation"
    )

    // Total bounding box reserving clearance for flat drop shadow
    Box(
        modifier = modifier
            .size(size + maxShadow)
            .padding(end = maxShadow - currentShadow, bottom = maxShadow - currentShadow)
    ) {
        // Flat Black Drop Shadow
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = currentShadow, y = currentShadow)
                .background(ComicTokens.SolidBlack, shape)
        )

        // Animated Button Face
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = currentTranslation, y = currentTranslation)
                .comicBorder(
                    width = ComicShapeTokens.BorderWidthDefault,
                    color = ComicTokens.SolidBlack,
                    shape = shape
                )
                .clip(shape)
                .background(backgroundColor, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        soundManager?.playClickSound()
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Step 5 (Day 23): Theme-dispatching Floating Action Button.
 *
 * Dispatches to [ComicFloatingActionButton] when [PixelTheme.mode] is [ThemeMode.Comic],
 * and renders classic 8-bit [FloatingActionButton] for Pixel and Light modes.
 */
@Composable
fun PixelFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {
        val colors = PixelTheme.colors
        Text(
            text = "+",
            style = PixelTypography.displaySmall,
            color = colors.onPrimary
        )
    }
) {
    val activeMode = PixelTheme.mode
    if (activeMode == ThemeMode.Comic) {
        ComicFloatingActionButton(
            onClick = onClick,
            modifier = modifier
        )
        return
    }

    val colors = PixelTheme.colors
    FloatingActionButton(
        onClick = onClick,
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
        shape = CutCornerShape(4.dp),
        modifier = modifier
    ) {
        content()
    }
}

@Preview(name = "Comic FAB Preview", showBackground = true)
@Composable
private fun ComicFabPreview() {
    ComicFloatingActionButton(onClick = {})
}
