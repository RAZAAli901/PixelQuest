package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Comic panel style variants matching Nitnode reference aesthetics:
 * - SURFACE: Crisp white panel fill
 * - BURNT_ORANGE: Warm action container
 * - SKY_BLUE: Energetic cool container
 * - LAVENDER: Accent mystery container
 * - PAPER: Warm neutral newsprint canvas
 */
enum class ComicPanelVariant {
    SURFACE,
    BURNT_ORANGE,
    SKY_BLUE,
    LAVENDER,
    PAPER
}

/**
 * Step 9: ComicPanel base composable combining border + flat drop-shadow + corner radius + solid background.
 * The Comic Mode equivalent of [PixelPanel] / [PixelCard].
 *
 * Automatically reserves shadow clearance (`padding(end = shadowOffset, bottom = shadowOffset)`)
 * so the duplicated flat black drop shadow renders within layout boundaries across all sizes without clipping.
 */
@Composable
fun ComicPanel(
    modifier: Modifier = Modifier,
    variant: ComicPanelVariant = ComicPanelVariant.SURFACE,
    backgroundColor: Color? = null,
    borderColor: Color = Color.Black,
    borderWidth: Dp = ComicShapeTokens.BorderWidthDefault,
    shadowColor: Color = Color.Black,
    shadowOffset: Dp = ComicShapeTokens.ShadowOffsetDefault,
    cornerRadius: Dp = ComicShapeTokens.RadiusDefault,
    contentPadding: Dp = 16.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val resolvedBg = backgroundColor ?: when (variant) {
        ComicPanelVariant.SURFACE -> ComicTokens.PanelSurface
        ComicPanelVariant.BURNT_ORANGE -> ComicTokens.BurntOrange
        ComicPanelVariant.SKY_BLUE -> ComicTokens.SkyBlue
        ComicPanelVariant.LAVENDER -> ComicTokens.Lavender
        ComicPanelVariant.PAPER -> ComicTokens.PaperBackground
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .comicDropShadow(
                offsetX = shadowOffset,
                offsetY = shadowOffset,
                color = shadowColor,
                shape = shape
            )
            .background(color = resolvedBg, shape = shape)
            .comicBorder(
                width = borderWidth,
                color = borderColor,
                shape = shape
            )
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}
