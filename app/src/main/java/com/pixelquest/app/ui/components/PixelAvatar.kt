package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.pqShape

/**
 * The player's hero, as UI v2 frames it: the class's own pixel sprite, drawn sharp (no smoothing),
 * on the avatar ground with an ink border.
 */
@Composable
fun PixelAvatar(avatar: AvatarClass, size: Dp, modifier: Modifier = Modifier) {
    val shape = pqShape(size * 0.16f)
    Box(
        modifier.size(size).clip(shape).background(PqColors.AvatarBg).border(size * 0.04f, PqColors.Ink, shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(avatar.sprite),
            contentDescription = stringResource(avatar.label),
            filterQuality = FilterQuality.None,
            modifier = Modifier.padding(size * 0.1f).size(size * 0.8f)
        )
    }
}
