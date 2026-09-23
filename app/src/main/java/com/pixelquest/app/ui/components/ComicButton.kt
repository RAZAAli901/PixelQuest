package com.pixelquest.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder

/**
 * Visual variant options for ComicButton.
 */
enum class ComicButtonVariant {
    PRIMARY,        // Coral Red CTA (#FF5A4E)
    BURNT_ORANGE,   // Burnt Orange (#F0A868)
    SKY_BLUE,       // Sky Blue (#8ECAE6)
    LAVENDER,       // Lavender (#B8A4D4)
    SURFACE         // White Surface (#FFFFFF)
}

/**
 * Mechanical press-physics specifications for comic buttons.
 * Resting: 4dp shadow, 0dp translation.
 * Pressed: 1dp shadow, +3dp translation down-right into shadow.
 */
object ComicButtonPhysics {
    val MaxShadow: Dp = ComicShapeTokens.ShadowOffsetDefault // 4.dp
    val MinShadow: Dp = 1.dp
    val TranslationDelta: Dp = MaxShadow - MinShadow // 3.dp
    const val AnimationDurationMs: Int = 60
}

/**
 * Step 28: Proof-of-concept comic-styled button asset/composable.
 * Implements Compose-drawn vector geometry: solid color fill, solid black ink border,
 * and hard-edged flat offset drop-shadow with tactile press physics.
 */
@Composable
fun ComicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ComicButtonVariant = ComicButtonVariant.PRIMARY,
    textColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    val soundManager = LocalSoundManager.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val maxShadow: Dp = ComicButtonPhysics.MaxShadow
    val minShadow: Dp = ComicButtonPhysics.MinShadow

    // Tactile press animation: button moves +3dp into shadow, shadow collapses
    val animatedShadowOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) minShadow else if (!enabled) minShadow else maxShadow,
        animationSpec = tween(durationMillis = ComicButtonPhysics.AnimationDurationMs),
        label = "comic_btn_shadow"
    )

    val animatedTranslation by animateDpAsState(
        targetValue = if (isPressed && enabled) ComicButtonPhysics.TranslationDelta else 0.dp,
        animationSpec = tween(durationMillis = ComicButtonPhysics.AnimationDurationMs),
        label = "comic_btn_translation"
    )

    val fillColor = if (!enabled) {
        Color(0xFFE0DDD5)
    } else {
        when (variant) {
            ComicButtonVariant.PRIMARY -> ComicTokens.CoralRed
            ComicButtonVariant.BURNT_ORANGE -> ComicTokens.BurntOrange
            ComicButtonVariant.SKY_BLUE -> ComicTokens.SkyBlue
            ComicButtonVariant.LAVENDER -> ComicTokens.Lavender
            ComicButtonVariant.SURFACE -> ComicTokens.PanelSurface
        }
    }

    val contentColor = if (!enabled) {
        Color(0xFF757575)
    } else if (textColor != Color.Unspecified) {
        textColor
    } else {
        ComicTokens.SolidBlack
    }

    val borderColor = if (!enabled) ComicTokens.SolidBlack.copy(alpha = 0.5f) else ComicTokens.SolidBlack
    val shadowColor = if (!enabled) ComicTokens.SolidBlack.copy(alpha = 0.35f) else ComicTokens.SolidBlack

    val shape = RoundedCornerShape(ComicShapeTokens.RadiusDefault)

    // Outer bounding Box reserves shadow clearance so layout never clips
    Box(
        modifier = modifier
            .padding(end = maxShadow, bottom = maxShadow)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom tactile translation handles indication
                enabled = enabled,
                role = Role.Button
            ) {
                PixelHaptics.performLightTap(haptic)
                soundManager?.playClickSound()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Flat solid black drop shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = animatedShadowOffset, y = animatedShadowOffset)
                .background(shadowColor, shape = shape)
        )

        // Main button surface face
        Box(
            modifier = Modifier
                .offset(x = animatedTranslation, y = animatedTranslation)
                .background(fillColor, shape = shape)
                .comicBorder(
                    width = ComicShapeTokens.BorderWidthDefault,
                    color = borderColor,
                    shape = shape
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}
