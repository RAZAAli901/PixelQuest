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

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

enum class PixelPanelVariant {
    BORDER,
    BLUE,
    BEIGE,
    LAVENDER
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    variant: PixelPanelVariant = PixelPanelVariant.BORDER,
    comicVariant: ComicPanelVariant? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val activeMode = PixelTheme.mode
    val colors = PixelTheme.colors

    if (activeMode == ThemeMode.Comic) {
        val resolvedComicVariant = comicVariant ?: when (variant) {
            PixelPanelVariant.BORDER -> ComicPanelVariant.SURFACE
            PixelPanelVariant.BLUE -> ComicPanelVariant.SKY_BLUE
            PixelPanelVariant.BEIGE -> ComicPanelVariant.BURNT_ORANGE
            PixelPanelVariant.LAVENDER -> ComicPanelVariant.LAVENDER
        }
        ComicPanel(
            modifier = modifier,
            variant = resolvedComicVariant,
            contentPadding = contentPadding
        ) {
            content()
        }
        return
    }

    if (activeMode == ThemeMode.Light) {
        val (cardBg, borderColor) = when (variant) {
            PixelPanelVariant.BORDER -> colors.surface to colors.pixelBorder
            PixelPanelVariant.BLUE -> colors.secondaryContainer to colors.secondary
            PixelPanelVariant.BEIGE -> colors.primaryContainer to colors.primary
            PixelPanelVariant.LAVENDER -> Color(0xFFEDE9FE) to Color(0xFF7C3AED)
        }

        Box(
            modifier = modifier
                .drawBehind {
                    val shadowOffset = 2.dp.toPx()
                    val borderWidth = 2.dp.toPx()
                    // Draw bottom-right 8-bit hard pixel shadow
                    drawRect(
                        color = Color(0x24000000),
                        topLeft = Offset(shadowOffset, shadowOffset),
                        size = size
                    )
                    // Draw card background surface
                    drawRect(
                        color = cardBg,
                        topLeft = Offset.Zero,
                        size = size
                    )
                    // Draw outer pixel border
                    drawRect(
                        color = borderColor,
                        topLeft = Offset.Zero,
                        size = size,
                        style = Stroke(width = borderWidth)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.padding(contentPadding)
            ) {
                content()
            }
        }
    } else {
        val bgRes = when (variant) {
            PixelPanelVariant.BORDER -> R.drawable.pixel_panel_border
            PixelPanelVariant.BLUE -> R.drawable.pixel_panel_blue
            PixelPanelVariant.BEIGE -> R.drawable.pixel_panel_beige
            PixelPanelVariant.LAVENDER -> R.drawable.pixel_panel_blue
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
}

/**
 * PixelPanel alias providing theme-aware card panel rendering.
 */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    variant: PixelPanelVariant = PixelPanelVariant.BORDER,
    comicVariant: ComicPanelVariant? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    PixelCard(
        modifier = modifier,
        variant = variant,
        comicVariant = comicVariant,
        contentPadding = contentPadding,
        content = content
    )
}


