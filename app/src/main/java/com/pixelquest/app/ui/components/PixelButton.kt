package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/** A button's kind, from before UI v2: the main one (now red), or a quieter one (now paper). */
enum class PixelButtonVariant {
    YELLOW,
    BLUE
}

/**
 * A button, as the screens from before UI v2 ask for one: now the skin's own (docs/ui-v2.md). The
 * main kind is red, the other paper; the text is as given. It clicks and buzzes as before.
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PixelButtonVariant = PixelButtonVariant.YELLOW,
    textColor: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    LegacyButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        fill = if (variant == PixelButtonVariant.YELLOW) PqColors.Red else PqColors.Paper,
        textColor = if (textColor != Color.Unspecified) textColor else if (variant == PixelButtonVariant.YELLOW) PqColors.OnRed else PqColors.Ink,
        enabled = enabled
    )
}

/** The buttons from before UI v2, drawn as the skin's: ink, its corners, its shadow, and a press. */
@Composable
internal fun LegacyButton(text: String, onClick: () -> Unit, modifier: Modifier, fill: Color, textColor: Color, enabled: Boolean) {
    val sound = LocalSoundManager.current
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .comicPressable(
                {
                    PixelHaptics.performLightTap(haptic)
                    sound?.playClickSound()
                    onClick()
                },
                bg = fill, radius = 12.dp, enabled = enabled, role = Role.Button
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = PqType.Button.copy(color = textColor), textAlign = TextAlign.Center)
    }
}
