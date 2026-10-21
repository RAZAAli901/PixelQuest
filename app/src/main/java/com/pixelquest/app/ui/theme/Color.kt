package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * The retro arcade palette's names, kept for the screens from before UI v2: each now gives the v2
 * colour for its role in the active skin ([PqColors]). The arcade's dark ground is v2's light one,
 * so its "white" text is v2's ink. New code uses PqColors.
 */

val PixelGold: Color get() = PqColors.Gold
val PixelGoldDark: Color get() = PqColors.Amber
val PixelCyan: Color get() = PqColors.Sky
val PixelCyanDark: Color get() = PqColors.StatusPerfect
val PixelGreen: Color get() = PqColors.Live
val PixelGreenDark: Color get() = PqColors.Live
val PixelRed: Color get() = PqColors.Error
val PixelPurple: Color get() = PqColors.Lilac

val PixelBackgroundDark: Color get() = PqColors.Ground
val PixelSurfaceDark: Color get() = PqColors.Paper
val PixelSurfaceBorder: Color get() = PqColors.Divider
val PixelTextWhite: Color get() = PqColors.Ink
val PixelTextMuted: Color get() = PqColors.Muted
val PixelBlack: Color get() = PqColors.Ink
