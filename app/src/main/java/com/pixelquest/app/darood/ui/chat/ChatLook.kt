package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * How a party's chat draws its pieces in the current look: the bubbles' corners and outlines, the
 * pills', and the number field's. One place, so the whole chat changes together.
 *
 * @param cut chamfered corners, as pixel art draws them, instead of rounded ones.
 * @param tailCorner the corner nearest a run's first bubble's side, drawn in as its tail.
 * @param outline the width of every piece's outline; 0 for none.
 */
@Immutable
data class ChatLook(
    val cut: Boolean,
    val corner: Dp,
    val tailCorner: Dp,
    val outline: Dp,
    val myOutline: Color,
    val theirOutline: Color,
    val pillOutline: Color
) {
    fun bubbleShape(mine: Boolean, tail: Boolean): Shape {
        val near = if (tail) tailCorner else corner
        val topStart = if (!mine) near else corner
        val topEnd = if (mine) near else corner
        return if (cut) {
            CutCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = corner, bottomStart = corner)
        } else {
            RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = corner, bottomStart = corner)
        }
    }

    val pillShape: Shape get() = if (cut) CutCornerShape(4.dp) else RoundedCornerShape(50)

    val fieldShape: Shape get() = if (cut) CutCornerShape(6.dp) else RoundedCornerShape(24.dp)

    /** The outline for a piece of [shape], in [color]; nothing in a look without outlines. */
    fun outlined(shape: Shape, color: Color): Modifier =
        if (outline > 0.dp) Modifier.border(outline, color, shape) else Modifier
}

/** The chat's look for the current theme. */
@Composable
fun chatLook(): ChatLook {
    val colors = PixelTheme.colors
    return when (PixelTheme.mode) {
        // Pixel: chamfered corners and 2dp outlines, as its panels have; square tails.
        ThemeMode.Pixel -> ChatLook(
            cut = true,
            corner = 6.dp,
            tailCorner = 0.dp,
            outline = 2.dp,
            myOutline = colors.primaryContainer,
            theirOutline = colors.pixelBorder,
            pillOutline = colors.pixelBorder
        )
        else -> ChatLook(
            cut = false,
            corner = 16.dp,
            tailCorner = 4.dp,
            outline = 0.dp,
            myOutline = Color.Transparent,
            theirOutline = Color.Transparent,
            pillOutline = Color.Transparent
        )
    }
}
