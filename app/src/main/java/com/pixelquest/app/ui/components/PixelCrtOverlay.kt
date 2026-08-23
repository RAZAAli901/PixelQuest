package com.pixelquest.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PixelCrtOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val scanlineStep = 4.dp.toPx()

                // Draw scanlines
                var y = 0f
                val scanlineColor = Color.Black.copy(alpha = 0.12f)
                val lineStrokeWidth = 1.5f.dp.toPx()

                while (y < canvasHeight) {
                    drawLine(
                        color = scanlineColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = lineStrokeWidth
                    )
                    y += scanlineStep
                }

                // Draw subtle vignette gradient overlay
                val vignetteRadius = maxOf(canvasWidth, canvasHeight) * 0.75f
                val vignetteBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.35f)
                    ),
                    center = Offset(canvasWidth / 2f, canvasHeight / 2f),
                    radius = vignetteRadius
                )

                drawRect(
                    brush = vignetteBrush,
                    size = Size(canvasWidth, canvasHeight)
                )
            }
        }
    }
}
