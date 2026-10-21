package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens

/**
 * The add button that floats over a screen from before UI v2: the skin's own (docs/ui-v2.md), red,
 * with a hard shadow it sinks into when pressed. It clicks and buzzes.
 */
@Composable
fun PixelFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { Icon(Icons.Rounded.Add, contentDescription = null, tint = PqColors.OnRed, modifier = Modifier.size(28.dp)) }
) {
    val sound = LocalSoundManager.current
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .comicPressable(
                {
                    PixelHaptics.performLightTap(haptic)
                    sound?.playClickSound()
                    onClick()
                },
                bg = PqColors.Red,
                radius = 16.dp,
                shadow = PqDimens.ShadowLg,
                role = Role.Button
            )
    ) {
        content()
    }
}
