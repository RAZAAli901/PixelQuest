package com.pixelquest.app.quran.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reader's text styles: Amiri Quran right to left with room for its marks, and a plain translation. */
class QuranTypographyTest {

    @Test
    fun theArabic_isAmiriQuran_rightToLeft_withRoomBetweenLines() {
        val arabic = QuranTypography.of().arabic

        assertEquals(AmiriQuran, arabic.fontFamily)
        assertEquals(TextDirection.Rtl, arabic.textDirection)
        assertEquals(QuranTypography.ARABIC_DEFAULT.sp, arabic.fontSize)
        assertTrue("twice its size, for the marks", arabic.lineHeight.value >= arabic.fontSize.value * 2)
        assertEquals("ar", arabic.localeList!!.first().language)
    }

    @Test
    fun theTranslation_isPlain_leftToRight_neverThePixelOrComicFont() {
        val translation = QuranTypography.of().translation

        assertEquals(FontFamily.SansSerif, translation.fontFamily)
        assertNotEquals(AmiriQuran, translation.fontFamily)
        assertEquals(TextDirection.Ltr, translation.textDirection)
        assertTrue(translation.lineHeight.value >= translation.fontSize.value * 1.5f)
    }

    @Test
    fun thePlayersSizes_areUsed() {
        val styles = QuranTypography.of(arabicSize = 36f, translationSize = 20f, lineSpacing = 1.2f)

        assertEquals(36.sp, styles.arabic.fontSize)
        assertEquals(20.sp, styles.translation.fontSize)
        assertEquals((36f * 2 * 1.2f).sp, styles.arabic.lineHeight)
    }

    @Test
    fun sizesAndSpacing_stayWithinTheirRanges() {
        val small = QuranTypography.of(arabicSize = 5f, translationSize = 1f, lineSpacing = 0.1f)
        val large = QuranTypography.of(arabicSize = 99f, translationSize = 99f, lineSpacing = 9f)

        assertEquals(QuranTypography.ARABIC_MIN.sp, small.arabic.fontSize)
        assertEquals(QuranTypography.TRANSLATION_MIN.sp, small.translation.fontSize)
        assertEquals((QuranTypography.ARABIC_MIN * 2 * QuranTypography.SPACING_MIN).sp, small.arabic.lineHeight)
        assertEquals(QuranTypography.ARABIC_MAX.sp, large.arabic.fontSize)
        assertEquals(QuranTypography.TRANSLATION_MAX.sp, large.translation.fontSize)
        assertEquals((QuranTypography.ARABIC_MAX * 2 * QuranTypography.SPACING_MAX).sp, large.arabic.lineHeight)
    }
}
