package com.pixelquest.app.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * How a row in the Quran's lists (surahs, juz, bookmarks, search results) is framed in the look:
 * its shape, its fill, its border, if it has one, and its flat shadow, if it has one.
 */
@Immutable
data class QuranFrame(val shape: Shape, val fill: Color, val border: BorderStroke?, val shadow: Dp? = null)

/**
 * The look's frame for a row: in Pixel, chamfered corners and the pixel border, as Pixel's panels
 * have; in Comic, an inked outline and a flat black shadow down and right, as Comic's panels have.
 * Elsewhere, for now, a plain surface.
 */
@Composable
fun quranFrame(): QuranFrame {
    val colors = PixelTheme.colors
    return when (PixelTheme.mode) {
        ThemeMode.Pixel, ThemeMode.System -> QuranFrame(CutCornerShape(6.dp), colors.surface, BorderStroke(2.dp, colors.pixelBorder))
        ThemeMode.Comic -> QuranFrame(
            shape = ComicShapeTokens.ShapeDefault,
            fill = colors.surface,
            border = BorderStroke(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack),
            shadow = ComicShapeTokens.ShadowOffsetSmall
        )
        else -> QuranFrame(MaterialTheme.shapes.small, colors.surface, null)
    }
}

/**
 * This row in [frame]: its shadow behind it, then filled and bordered in its shape. Not clipped to
 * it: a clip to Pixel's chamfered shape (a path) would test every tap against the path, and the
 * rows were never clipped.
 */
fun Modifier.framed(frame: QuranFrame): Modifier {
    val shadowed = if (frame.shadow != null) comicDropShadow(offsetX = frame.shadow, offsetY = frame.shadow, shape = frame.shape) else this
    val filled = shadowed.background(frame.fill, frame.shape)
    return if (frame.border != null) filled.border(frame.border, frame.shape) else filled
}
