package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Retro cathode-ray tube (CRT) monitor scanline and vignette overlay.
 *
 * Architecture Decision (Day 16):
 * This filter is intentionally restricted to [com.pixelquest.app.ui.theme.ThemeMode.Pixel] only.
 * In Light and Comic modes, scanlines degrade readability and contradict the medium's aesthetic
 * (paper print / crisp modern interface). Thus, it is automatically bypassed when non-Pixel themes are active.
 */
@Composable
fun PixelCrtOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (enabled) {
                    // Built once per size, not per frame: the overlay covers the whole app, so every
                    // frame of a scroll or a tab's slide draws it. One path of scanlines and one
                    // gradient rect, instead of a line per 4dp and a new brush each time.
                    Modifier.drawWithCache {
                        val scanlineStep = 4.dp.toPx()
                        val lineHeight = 1.5f.dp.toPx()
                        val scanlines = Path().apply {
                            var y = 0f
                            while (y < size.height) {
                                addRect(Rect(0f, y - lineHeight / 2, size.width, y + lineHeight / 2))
                                y += scanlineStep
                            }
                        }
                        val vignette = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.35f)),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = maxOf(size.width, size.height) * 0.75f
                        )
                        onDrawWithContent {
                            drawContent()
                            drawPath(scanlines, Color.Black.copy(alpha = 0.12f))
                            drawRect(brush = vignette, size = Size(size.width, size.height))
                        }
                    }
                } else Modifier
            )
    ) {
        content()
    }
}
