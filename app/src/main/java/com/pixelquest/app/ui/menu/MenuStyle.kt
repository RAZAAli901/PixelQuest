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
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
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
    val ink: Color,
    /** A flat black shadow under the current entry, like the Comic tabs' pill. */
    val selectedShadow: Boolean = false
)

@Composable
fun menuStyle(): MenuStyle = when (PixelTheme.mode) {
    ThemeMode.Comic -> comicMenuStyle()
    else -> pixelMenuStyle()
}

/**
 * Comic: a 2.5dp black ink border with rounded outer corners, entries in the comic font, and the
 * current one a coral pill with a black outline and a flat shadow, matching the Comic tab bar.
 */
@Composable
private fun comicMenuStyle(): MenuStyle = MenuStyle(
    sheetShape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
    sheetBorder = BorderStroke(2.5.dp, ComicTokens.SolidBlack),
    itemShape = RoundedCornerShape(10.dp),
    selectedFill = ComicTokens.CoralRed,
    selectedBorder = BorderStroke(2.dp, ComicTokens.SolidBlack),
    labelStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = BangersFontFamily, letterSpacing = 0.6.sp),
    uppercaseLabels = true,
    closeIcon = R.drawable.ic_close_comic,
    ink = ComicTokens.SolidBlack,
    selectedShadow = true
)

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
