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
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * How a row in the Quran's lists (surahs, juz, bookmarks, search results) is framed in the look:
 * its shape, its fill, and its border, if it has one.
 */
@Immutable
data class QuranFrame(val shape: Shape, val fill: Color, val border: BorderStroke?)

/**
 * The look's frame for a row: in Pixel, chamfered corners and the pixel border, as Pixel's panels
 * have. Elsewhere, for now, a plain surface.
 */
@Composable
fun quranFrame(): QuranFrame {
    val colors = PixelTheme.colors
    return when (PixelTheme.mode) {
        ThemeMode.Pixel, ThemeMode.System -> QuranFrame(CutCornerShape(6.dp), colors.surface, BorderStroke(2.dp, colors.pixelBorder))
        else -> QuranFrame(MaterialTheme.shapes.small, colors.surface, null)
    }
}

/**
 * This row in [frame]: filled and bordered in its shape. Not clipped to it: a clip to Pixel's
 * chamfered shape (a path) would test every tap against the path, and the rows were never clipped.
 */
fun Modifier.framed(frame: QuranFrame): Modifier {
    val filled = background(frame.fill, frame.shape)
    return if (frame.border != null) filled.border(frame.border, frame.shape) else filled
}
