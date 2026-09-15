package com.pixelquest.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint

/**
 * Step 33: Asset tinting and color-filter utility.
 * Allows reusing existing pixel-art PNG/vector assets across themes (notably Light Mode)
 * without generating redundant raster assets or inflating the APK size.
 */
object PixelThemeAssetFilter {

    /**
     * Creates a ColorFilter for tinting pixel assets.
     * In Pixel mode, returns null by default to preserve original 8-bit palette colors.
     * In Light mode, tints the asset with the specified target color.
     */
    fun forTheme(
        themeMode: ThemeMode,
        lightColor: Color,
        blendMode: BlendMode = BlendMode.SrcAtop
    ): ColorFilter? = when (themeMode) {
        ThemeMode.Pixel -> null // Preserve original canonical pixel asset colors
        ThemeMode.Light -> ColorFilter.tint(lightColor, blendMode)
        ThemeMode.Comic -> null // Comic uses dedicated assets, null fallback
        ThemeMode.System -> null // Resolved at runtime
    }

    /**
     * Tinting ColorFilter for buttons and framed panels.
     */
    fun buttonTint(
        themeMode: ThemeMode,
        targetTint: Color
    ): ColorFilter? = if (themeMode == ThemeMode.Light) {
        ColorFilter.tint(targetTint, BlendMode.SrcAtop)
    } else {
        null
    }

    /**
     * High-contrast grayscale filter for disabled or unearned game badges.
     */
    fun grayscale(): ColorFilter {
        val matrix = ColorMatrix().apply { setToSaturation(0f) }
        return ColorFilter.colorMatrix(matrix)
    }

    /**
     * Color invert filter for high-visibility dark/light adaptations.
     */
    fun invert(): ColorFilter {
        val invertMatrix = ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )
        )
        return ColorFilter.colorMatrix(invertMatrix)
    }
}
