package com.pixelquest.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The ink, corners and shadow a comic border or shadow gets unless told otherwise: the skin's. */
val DefaultComicBorderWidth: Dp get() = PqDimens.Border
val DefaultComicBorderColor: Color get() = PqColors.Ink
val DefaultComicCornerRadius: Dp get() = if (isPixelSkin) 0.dp else 10.dp
val DefaultComicShadowOffsetX: Dp get() = PqDimens.Shadow
val DefaultComicShadowOffsetY: Dp get() = PqDimens.Shadow
val DefaultComicShadowColor: Color get() = PqColors.Ink

/**
 * Step 6: Reusable comicBorder modifier.
 * Applies a crisp, solid black ink border around components with defined corner radius or shape.
 */
fun Modifier.comicBorder(
    width: Dp = DefaultComicBorderWidth,
    color: Color = DefaultComicBorderColor,
    shape: Shape = pqShape(10.dp)
): Modifier = this.border(
    width = width,
    color = color,
    shape = shape
)

/**
 * Overload for comicBorder taking a direct corner radius in Dp.
 */
fun Modifier.comicBorder(
    width: Dp = DefaultComicBorderWidth,
    color: Color = DefaultComicBorderColor,
    cornerRadius: Dp
): Modifier = this.border(
    width = width,
    color = color,
    shape = pqShape(cornerRadius)
)

/**
 * Step 7: Reusable comicDropShadow modifier.
 * Draws a hard-edged, flat, solid black shadow offset down-right behind the component.
 * This is a comic-panel drop shadow (a literal duplicated offset shape), not a blurred Material elevation shadow.
 * In the Pixel skin it is a straight step down, as UI v2 draws it.
 */
fun Modifier.comicDropShadow(
    offsetX: Dp = DefaultComicShadowOffsetX,
    offsetY: Dp = DefaultComicShadowOffsetY,
    color: Color = DefaultComicShadowColor,
    shape: Shape = pqShape(10.dp)
): Modifier = this.drawBehind {
    val dx = if (isPixelSkin) 0f else offsetX.toPx()
    val dy = offsetY.toPx()
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> {
            drawRect(
                color = color,
                topLeft = Offset(dx, dy),
                size = outline.rect.size
            )
        }
        is Outline.Rounded -> {
            val rr = outline.roundRect
            drawRoundRect(
                color = color,
                topLeft = Offset(dx, dy),
                size = Size(rr.width, rr.height),
                cornerRadius = CornerRadius(rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y)
            )
        }
        is Outline.Generic -> {
            drawContext.canvas.save()
            drawContext.canvas.translate(dx, dy)
            drawPath(path = outline.path, color = color)
            drawContext.canvas.restore()
        }
    }
}

/**
 * Overload for comicDropShadow taking direct corner radius in Dp.
 */
fun Modifier.comicDropShadow(
    offsetX: Dp = DefaultComicShadowOffsetX,
    offsetY: Dp = DefaultComicShadowOffsetY,
    color: Color = DefaultComicShadowColor,
    cornerRadius: Dp
): Modifier = comicDropShadow(
    offsetX = offsetX,
    offsetY = offsetY,
    color = color,
    shape = pqShape(cornerRadius)
)
