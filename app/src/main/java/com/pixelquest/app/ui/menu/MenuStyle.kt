package com.pixelquest.app.ui.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

/** How the side menu looks in the current theme. */
@Immutable
data class MenuStyle(
    val sheetShape: Shape,
    val sheetBorder: BorderStroke?,
    val itemShape: Shape,
    val selectedFill: Color,
    val selectedBorder: BorderStroke?,
    val labelStyle: TextStyle,
    val uppercaseLabels: Boolean,
    @DrawableRes val closeIcon: Int,
    val ink: Color
)

@Composable
fun menuStyle(): MenuStyle = pixelMenuStyle()

/**
 * Pixel: square corners everywhere, a pixel border down the menu's edge, the retro font for the
 * entries, and a bordered block for the current one, like the app's pixel panels and tabs.
 */
@Composable
private fun pixelMenuStyle(): MenuStyle {
    val colors = PixelTheme.colors
    return MenuStyle(
        sheetShape = RectangleShape,
        sheetBorder = BorderStroke(3.dp, colors.pixelBorder),
        itemShape = RectangleShape,
        selectedFill = colors.primary.copy(alpha = 0.18f),
        selectedBorder = BorderStroke(2.dp, colors.primary),
        labelStyle = PixelTypography.labelLarge,
        uppercaseLabels = true,
        closeIcon = R.drawable.ic_close_pixel,
        ink = colors.primaryText
    )
}

/** Before a theme has its own: rounded rows with a soft tint for the current one. */
@Composable
internal fun plainMenuStyle(): MenuStyle {
    val colors = PixelTheme.colors
    return MenuStyle(
        sheetShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        sheetBorder = null,
        itemShape = RoundedCornerShape(12.dp),
        selectedFill = colors.primary.copy(alpha = 0.16f),
        selectedBorder = null,
        labelStyle = MaterialTheme.typography.titleSmall,
        uppercaseLabels = false,
        closeIcon = R.drawable.ic_close_pixel,
        ink = colors.primaryText
    )
}
