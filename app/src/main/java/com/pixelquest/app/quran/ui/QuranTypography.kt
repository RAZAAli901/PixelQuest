package com.pixelquest.app.quran.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R

/** Amiri Quran (licenses/amiri-quran-OFL.txt): made for the Quran's text, its marks and its ayah signs. */
val AmiriQuran = FontFamily(Font(R.font.amiri_quran))

/**
 * The reader's text: the Arabic in Amiri Quran, right to left, with room between its lines for the
 * marks above and below them; the translation in the phone's plain sans-serif, left to right. Never
 * the pixel or comic fonts, whatever the theme: the Quran's text must read clearly.
 */
@Immutable
data class QuranTypography(val arabic: TextStyle, val translation: TextStyle) {

    companion object {
        /** The Arabic's size, in sp: the player chooses within these. */
        const val ARABIC_MIN = 20f
        const val ARABIC_DEFAULT = 28f
        const val ARABIC_MAX = 44f

        /** The translation's size, in sp. */
        const val TRANSLATION_MIN = 12f
        const val TRANSLATION_DEFAULT = 16f
        const val TRANSLATION_MAX = 28f

        /** Line spacing, as a share of the usual: 1 is the reader's own, generous already. */
        const val SPACING_MIN = 0.8f
        const val SPACING_DEFAULT = 1f
        const val SPACING_MAX = 1.6f

        /** The Arabic's lines are this many times its size apart: room for its marks. */
        private const val ARABIC_LINE = 2f

        /** The translation's lines, as easy reading. */
        private const val TRANSLATION_LINE = 1.5f

        /** The styles for the player's [arabicSize], [translationSize] and [lineSpacing], each kept within its range. */
        fun of(
            arabicSize: Float = ARABIC_DEFAULT,
            translationSize: Float = TRANSLATION_DEFAULT,
            lineSpacing: Float = SPACING_DEFAULT,
            color: Color = Color.Unspecified
        ): QuranTypography {
            val arabic = arabicSize.coerceIn(ARABIC_MIN, ARABIC_MAX)
            val translation = translationSize.coerceIn(TRANSLATION_MIN, TRANSLATION_MAX)
            val spacing = lineSpacing.coerceIn(SPACING_MIN, SPACING_MAX)
            // Lines centred in their height, nothing trimmed: the marks keep their room above and below.
            val roomy = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)
            return QuranTypography(
                arabic = TextStyle(
                    fontFamily = AmiriQuran,
                    fontSize = arabic.sp,
                    lineHeight = (arabic * ARABIC_LINE * spacing).sp,
                    lineHeightStyle = roomy,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Start,
                    localeList = LocaleList("ar"),
                    color = color
                ),
                translation = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = translation.sp,
                    lineHeight = (translation * TRANSLATION_LINE * spacing).sp,
                    lineHeightStyle = roomy,
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Start,
                    color = color
                )
            )
        }
    }
}
