package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R

@Composable
fun PixelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode

    if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        ComicProgressBar(
            progress = progress,
            modifier = modifier,
            height = height
        )
        return
    }

    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Background
        Image(
            painter = painterResource(id = R.drawable.pixel_bar_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )

        // Fill Indicator
        if (clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedProgress)
                    .padding(horizontal = 3.dp, vertical = 3.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pixel_bar_green_fill),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = com.pixelquest.app.ui.theme.PixelThemeAssetFilter.forTheme(
                        activeMode,
                        colors.tertiary
                    ),
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}
