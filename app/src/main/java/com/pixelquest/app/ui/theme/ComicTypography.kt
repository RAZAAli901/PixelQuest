package com.pixelquest.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R

/**
 * Bold heavyweight display font for comic headlines and titles.
 */
val BangersFontFamily = FontFamily(
    Font(R.font.bangers_regular, FontWeight.Normal)
)

/**
 * Handwritten marker font reserved strictly for accent callouts, stickers, and dialogue bursts.
 */
val KalamFontFamily = FontFamily(
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold)
)

/**
 * Step 14: ComicTypography wiring fonts into their designated roles:
 * - Bold display font (Bangers) for headlines, titles, and hero displays
 * - Clean readable sans (FontFamily.SansSerif) for UI labels and body text
 * - Marker font reserved for accent/callout text in [ComicCalloutStyles]
 */
val ComicTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BangersFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 1.sp
    ),
    displayMedium = TextStyle(
        fontFamily = BangersFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.5.sp
    ),
    displaySmall = TextStyle(
        fontFamily = BangersFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BangersFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BangersFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = BangersFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BangersFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp
    )
)

/**
 * Handwritten/marker-style callout accents (matching reference "NO FLUFF, NO 12-PAGE REPORT...").
 * Reserved specifically for accent chips, action bursts, and sticky notes.
 */
object ComicCalloutStyles {
    val calloutLarge = TextStyle(
        fontFamily = KalamFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
    val calloutMedium = TextStyle(
        fontFamily = KalamFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val calloutSmall = TextStyle(
        fontFamily = KalamFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
}
