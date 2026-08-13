package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R

enum class PixelPanelVariant {
    BORDER,
    BLUE,
    BEIGE
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    variant: PixelPanelVariant = PixelPanelVariant.BORDER,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val bgRes = when (variant) {
        PixelPanelVariant.BORDER -> R.drawable.pixel_panel_border
        PixelPanelVariant.BLUE -> R.drawable.pixel_panel_blue
        PixelPanelVariant.BEIGE -> R.drawable.pixel_panel_beige
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = bgRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}
