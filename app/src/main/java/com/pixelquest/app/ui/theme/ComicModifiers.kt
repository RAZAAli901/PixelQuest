package com.pixelquest.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default comic stroke parameters extracted from Nitnode reference aesthetics:
 * Solid black, ~2.5dp stroke, never soft or anti-aliased.
 */
val DefaultComicBorderWidth: Dp = 2.5.dp
val DefaultComicBorderColor: Color = Color.Black
val DefaultComicCornerRadius: Dp = 10.dp

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
