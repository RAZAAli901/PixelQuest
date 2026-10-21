package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens

/** A panel's colour, from before UI v2: paper, amber, sky, lilac, or the ground ([fill]). */
enum class ComicPanelVariant {
    SURFACE,
    BURNT_ORANGE,
    SKY_BLUE,
    LAVENDER,
    PAPER
}

/** The fill UI v2 gives a panel of [this] variant. */
fun ComicPanelVariant.fill(): Color = when (this) {
    ComicPanelVariant.SURFACE -> PqColors.Paper
    ComicPanelVariant.BURNT_ORANGE -> PqColors.Amber
    ComicPanelVariant.SKY_BLUE -> PqColors.Sky
    ComicPanelVariant.LAVENDER -> PqColors.Lilac
    ComicPanelVariant.PAPER -> PqColors.Ground
}

/**
 * A comic panel, as the screens from before UI v2 ask for one: now the skin's own card
 * ([Modifier.comic]), in [variant]'s colour or [backgroundColor]. A panel asked for without a
 * shadow ([shadowOffset] 0) gets none; the ink and corners are the skin's.
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
    contentPadding: PaddingValues = PaddingValues(16.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .comic(bg = backgroundColor ?: variant.fill(), radius = PqDimens.RadiusLg, shadow = if (shadowOffset > 0.dp) PqDimens.Shadow else 0.dp)
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

/**
 * Convenient overload accepting uniform contentPadding in Dp.
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
    contentPadding: Dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    ComicPanel(
        modifier = modifier,
        variant = variant,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        shadowColor = shadowColor,
        shadowOffset = shadowOffset,
        cornerRadius = cornerRadius,
        contentPadding = PaddingValues(contentPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}
