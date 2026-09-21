package com.pixelquest.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Step 31: Validated and locked shape system tokens for Comic Book UI Mode,
 * refined against Nitnode reference visual specifications:
 * - Border width: 2.5dp solid black ink stroke
 * - Shadow offset: 4dp down-right flat unblurred duplicated shape
 * - Corner radius: 10dp for cards and buttons (8dp for chips, 12dp for hero banners)
 */
object ComicShapeTokens {
    // Exact Validated Corner Radii
    val RadiusSmall: Dp = 8.dp
    val RadiusDefault: Dp = 10.dp
    val RadiusLarge: Dp = 12.dp
    val RadiusPill: Dp = 999.dp

    // Component-Specific Radii
    val ButtonRadius: Dp = 10.dp
    val CardRadius: Dp = 10.dp
    val StatCardRadius: Dp = 10.dp
    val HeroRadius: Dp = 12.dp
    val ChipRadius: Dp = 8.dp

    // Predefined Shapes
    val ShapeSmall: Shape = RoundedCornerShape(RadiusSmall)
    val ShapeDefault: Shape = RoundedCornerShape(RadiusDefault)
    val ShapeLarge: Shape = RoundedCornerShape(RadiusLarge)
    val ShapePill: Shape = RoundedCornerShape(RadiusPill)

    // Validated Border Widths
    val BorderWidthThin: Dp = 2.dp
    val BorderWidthDefault: Dp = 2.5.dp
    val BorderWidthThick: Dp = 3.dp

    // Validated Drop Shadow Offsets
    val ShadowOffsetSmall: Dp = 3.dp
    val ShadowOffsetDefault: Dp = 4.dp
    val ShadowOffsetLarge: Dp = 6.dp

    // Invariant Colors
    val BorderColor: Color = Color.Black
    val ShadowColor: Color = Color.Black
}
