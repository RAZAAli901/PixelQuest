package com.pixelquest.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Step 8: Shape system tokens for Comic Book UI Mode.
 * Extracted and refined from Nitnode reference aesthetics:
 * - Corner radius: moderate, ~8-12dp
 * - Borders: solid black, ~2-3dp
 * - Shadows: hard-edged flat solid black offset down-right (~3-6dp)
 */
object ComicShapeTokens {
    // Corner Radius Tokens
    val RadiusSmall: Dp = 8.dp
    val RadiusDefault: Dp = 10.dp
    val RadiusLarge: Dp = 12.dp
    val RadiusPill: Dp = 999.dp

    // Predefined Shapes
    val ShapeSmall: Shape = RoundedCornerShape(RadiusSmall)
    val ShapeDefault: Shape = RoundedCornerShape(RadiusDefault)
    val ShapeLarge: Shape = RoundedCornerShape(RadiusLarge)
    val ShapePill: Shape = RoundedCornerShape(RadiusPill)

    // Border Width Tokens
    val BorderWidthThin: Dp = 2.dp
    val BorderWidthDefault: Dp = 2.5.dp
    val BorderWidthThick: Dp = 3.dp

    // Drop Shadow Offset Tokens
    val ShadowOffsetSmall: Dp = 3.dp
    val ShadowOffsetDefault: Dp = 4.dp
    val ShadowOffsetLarge: Dp = 6.dp
}
