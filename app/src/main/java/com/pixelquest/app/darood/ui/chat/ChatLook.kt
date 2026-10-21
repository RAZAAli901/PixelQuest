package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.isPixelSkin

/**
 * How a party's chat draws its pieces (mock 4E): ink round every bubble, pill and field, and a small
 * hard shadow under each, in the skin's corners (square in Pixel). One place, so the whole chat
 * changes together.
 *
 * @param corner a bubble's corners; 0 in the Pixel skin.
 * @param tailCorner the corner nearest a run's first bubble's side, drawn in as its tail.
 * @param outline the width of every piece's ink outline.
 * @param shadow how far a flat, hard-edged shadow sits under each piece (down and right in Comic,
 *   straight down in Pixel); 0 for none, as in Simple Mode.
 */
@Immutable
data class ChatLook(
    val corner: Dp,
    val tailCorner: Dp,
    val outline: Dp,
    val shadow: Dp
) {
    fun bubbleShape(mine: Boolean, tail: Boolean): Shape {
        val near = if (tail) tailCorner else corner
        val topStart = if (!mine) near else corner
        val topEnd = if (mine) near else corner
        return RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = corner, bottomStart = corner)
    }

    val pillShape: Shape get() = if (corner == 0.dp) RectangleShape else RoundedCornerShape(50)

    val fieldShape: Shape get() = if (corner == 0.dp) RectangleShape else RoundedCornerShape(12.dp)

    /** The ink outline for a piece of [shape]. */
    fun outlined(shape: Shape): Modifier = Modifier.border(outline, PqColors.Ink, shape)

    /**
     * The flat shadow behind a piece of [shape]; before any clip, so it shows. Nothing without one.
     * It's drawn from the piece's own outline, corner by corner, so a bubble's small tail corner shows
     * in its shadow too.
     */
    fun shadowed(shape: Shape): Modifier =
        if (shadow > 0.dp) {
            Modifier.drawBehind {
                val offset = shadow.toPx()
                translate(left = if (isPixelSkin) 0f else offset, top = offset) {
                    drawOutline(shape.createOutline(size, layoutDirection, this), PqColors.Ink)
                }
            }
        } else {
            Modifier
        }
}

/** Simple Mode in a party's chat, provided once at its root (PartyChatScreen). */
val LocalDaroodSimpleMode = staticCompositionLocalOf { false }

/** The chat's look in the active skin; Simple Mode keeps the ink and drops the shadows. */
@Composable
fun chatLook(): ChatLook {
    val pixel = isPixelSkin
    return ChatLook(
        corner = if (pixel) 0.dp else 16.dp,
        tailCorner = if (pixel) 0.dp else 4.dp,
        outline = PqDimens.BorderThin,
        shadow = if (LocalDaroodSimpleMode.current) 0.dp else 2.dp
    )
}
