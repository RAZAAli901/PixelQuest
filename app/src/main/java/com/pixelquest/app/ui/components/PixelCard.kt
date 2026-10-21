package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens

/** A card's colour, from before UI v2: plain, sky, gold or lilac. */
enum class PixelPanelVariant {
    BORDER,
    BLUE,
    BEIGE,
    LAVENDER
}

/** The fill UI v2 gives a card of [variant]. */
fun PixelPanelVariant.fill(): Color = when (this) {
    PixelPanelVariant.BORDER -> PqColors.Paper
    PixelPanelVariant.BLUE -> PqColors.Sky
    PixelPanelVariant.BEIGE -> PqColors.Gold
    PixelPanelVariant.LAVENDER -> PqColors.Lilac
}

/**
 * A card, as the screens from before UI v2 ask for one: now the skin's own card ([Modifier.comic]),
 * so those screens wear Comic or Pixel like the rest (docs/ui-v2.md). [variant] (or [comicVariant])
 * picks its colour.
 */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    variant: PixelPanelVariant = PixelPanelVariant.BORDER,
    comicVariant: ComicPanelVariant? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val fill = comicVariant?.fill() ?: variant.fill()
    Box(modifier.comic(bg = fill, radius = PqDimens.RadiusLg, shadow = PqDimens.Shadow)) {
        Box(Modifier.padding(contentPadding)) { content() }
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
