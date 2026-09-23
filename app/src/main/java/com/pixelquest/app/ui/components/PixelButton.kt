package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R

import com.pixelquest.app.audio.LocalSoundManager

enum class PixelButtonVariant {
    YELLOW,
    BLUE
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PixelButtonVariant = PixelButtonVariant.YELLOW,
    textColor: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode

    if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        val comicVariant = when (variant) {
            PixelButtonVariant.YELLOW -> ComicButtonVariant.PRIMARY
            PixelButtonVariant.BLUE -> ComicButtonVariant.SKY_BLUE
        }
        ComicButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            variant = comicVariant,
            enabled = enabled
        )
        return
    }

    val soundManager = LocalSoundManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgRes = when (variant) {
        PixelButtonVariant.YELLOW -> if (isPressed && enabled) R.drawable.pixel_button_yellow_pressed else R.drawable.pixel_button_yellow
        PixelButtonVariant.BLUE -> if (isPressed && enabled) R.drawable.pixel_button_blue_pressed else R.drawable.pixel_button_blue
    }

    val contentOffsetY = if (isPressed && enabled) 2.dp else 0.dp

    val tintColor = if (variant == PixelButtonVariant.YELLOW) {
        com.pixelquest.app.ui.theme.PixelTheme.colors.primary
    } else {
        com.pixelquest.app.ui.theme.PixelTheme.colors.secondary
    }
    val assetFilter = com.pixelquest.app.ui.theme.PixelThemeAssetFilter.buttonTint(activeMode, tintColor)

    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors
    val resolvedTextColor = if (textColor != Color.Unspecified) {
        textColor
    } else if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Light) {
        if (variant == PixelButtonVariant.YELLOW) colors.onPrimary else colors.onSecondary
    } else {
        if (variant == PixelButtonVariant.YELLOW) Color.Black else Color.White
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = if (!enabled) 0.5f else if (isPressed && activeMode == com.pixelquest.app.ui.theme.ThemeMode.Light) 0.92f else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = {
                    com.pixelquest.app.ui.haptics.PixelHaptics.performLightTap(haptic)
                    soundManager?.playClickSound()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = bgRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = assetFilter,
            modifier = Modifier.matchParentSize()
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = resolvedTextColor,
            modifier = Modifier
                .offset(y = contentOffsetY)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
