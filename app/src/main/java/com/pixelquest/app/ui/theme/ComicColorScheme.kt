package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The Comic colours, by the names the screens from before UI v2 use: each one is now the v2 colour
 * for its role in the active skin ([PqColors]), so those screens re-skin with the rest.
 */
object ComicTokens {
    val CoralRed: Color get() = PqColors.Red
    /** Primary-coloured text on paper and white (passes contrast, where [CoralRed] does not). */
    val CoralInk: Color get() = PqColors.RedText
    val BurntOrange: Color get() = PqColors.Amber
    val SkyBlue: Color get() = PqColors.Sky
    val Lavender: Color get() = PqColors.Lilac
    val SolidBlack: Color get() = PqColors.Ink
    val PaperBackground: Color get() = PqColors.Ground
    val PanelSurface: Color get() = PqColors.Paper
    val SurfaceVariant: Color get() = PqColors.Sunken
    val TextPrimary: Color get() = PqColors.Ink
    val TextSecondary: Color get() = PqColors.Ink2
    val GoldAccent: Color get() = PqColors.Gold
    val ErrorRed: Color get() = PqColors.Error
}
