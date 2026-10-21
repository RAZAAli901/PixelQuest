package com.pixelquest.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/** The display face (titles, numbers), as the screens from before UI v2 name it: the active skin's. */
val BangersFontFamily: FontFamily get() = PqSkinState.spec.display

/** Material's type scale as the Comic screens from before UI v2 name it: the active skin's ([pqTypography]). */
val ComicTypography: Typography get() = pqTypography()
