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

/**
 * Default comic stroke and shadow parameters extracted from Nitnode reference aesthetics:
 * - Solid black border (~2.5dp)
 * - Flat unblurred drop shadow offset down-right (4dp x 4dp)
 * - Moderate corner radius (~10dp)
 */
val DefaultComicBorderWidth: Dp = 2.5.dp
val DefaultComicBorderColor: Color = Color.Black
val DefaultComicCornerRadius: Dp = 10.dp
val DefaultComicShadowOffsetX: Dp = 4.dp
val DefaultComicShadowOffsetY: Dp = 4.dp
val DefaultComicShadowColor: Color = Color.Black

/**
 * Step 6: Reusable comicBorder modifier.
 * Applies a crisp, solid black ink border around components with defined corner radius or shape.
 */
fun Modifier.comicBorder(
    width: Dp = DefaultComicBorderWidth,
    color: Color = DefaultComicBorderColor,
    shape: Shape = RoundedCornerShape(DefaultComicCornerRadius)
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
    shape = RoundedCornerShape(cornerRadius)
)

/**
 * Step 7: Reusable comicDropShadow modifier.
 * Draws a hard-edged, flat, solid black shadow offset down-right behind the component.
 * This is a comic-panel drop shadow (a literal duplicated offset shape), not a blurred Material elevation shadow.
 */
fun Modifier.comicDropShadow(
    offsetX: Dp = DefaultComicShadowOffsetX,
    offsetY: Dp = DefaultComicShadowOffsetY,
    color: Color = DefaultComicShadowColor,
    shape: Shape = RoundedCornerShape(DefaultComicCornerRadius)
): Modifier = this.drawBehind {
    val dx = offsetX.toPx()
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
    shape = RoundedCornerShape(cornerRadius)
)
