package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The comic shape tokens, by the names the screens from before UI v2 use: each now follows the active
 * skin (docs/ui-v2.md), so corners are square in Pixel and the ink and shadow are v2's.
 */
object ComicShapeTokens {
    private fun r(dp: Dp): Dp = if (isPixelSkin) 0.dp else dp

    val RadiusSmall: Dp get() = r(8.dp)
    val RadiusDefault: Dp get() = r(10.dp)
    val RadiusLarge: Dp get() = r(12.dp)
    val RadiusPill: Dp get() = r(999.dp)

    val ButtonRadius: Dp get() = r(12.dp)
    val CardRadius: Dp get() = r(14.dp)
    val StatCardRadius: Dp get() = r(14.dp)
    val HeroRadius: Dp get() = r(16.dp)
    val ChipRadius: Dp get() = r(8.dp)

    val ShapeSmall: Shape get() = pqShape(8.dp)
    val ShapeDefault: Shape get() = pqShape(10.dp)
    val ShapeLarge: Shape get() = pqShape(12.dp)
    val ShapePill: Shape get() = pqShape(999.dp)

    val BorderWidthThin: Dp get() = PqDimens.BorderThin
    val BorderWidthDefault: Dp get() = PqDimens.Border
    val BorderWidthThick: Dp get() = PqDimens.Border + 0.5.dp

    val ShadowOffsetSmall: Dp get() = PqDimens.Shadow
    val ShadowOffsetDefault: Dp get() = PqDimens.Shadow
    val ShadowOffsetLarge: Dp get() = PqDimens.ShadowLg

    val BorderColor: Color get() = PqColors.Ink
    val ShadowColor: Color get() = PqColors.Ink
}
