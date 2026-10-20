package com.pixelquest.app.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The key Arabic search matches by: the same for a word typed plain or with its marks. */
class ArabicSearchTest {

    private fun char(code: Int) = Char(code).toString()

    // Kitab, typed plain, with its harakat, and as the Uthmani text spells it (wasla, a small alef above).
    private val kitabPlain = "الكتاب"
    private val kitabMarked = "الْكِتَابُ"
    private val kitabUthmani = char(0x0671) + "لْكِتَ" + char(0x0670) + "بُ"

    @Test
    fun marks_dontMatter() {
        assertEquals(ArabicSearch.key(kitabPlain), ArabicSearch.key(kitabMarked))
    }

    @Test
    fun theUthmaniSpelling_matchesTheEverydayOne() {
        assertEquals(ArabicSearch.key(kitabPlain), ArabicSearch.key(kitabUthmani))
    }

    @Test
    fun hamzasAndTheirLetters_becomePlain() {
        // Mu'min: waw with hamza is waw. Ta marbuta is heh; alef maksura is yeh.
        assertEquals(ArabicSearch.key("مومن"), ArabicSearch.key("مؤمن"))
        assertEquals(ArabicSearch.key("رحمه"), ArabicSearch.key("رحمة"))
        assertEquals(ArabicSearch.key("هدي"), ArabicSearch.key("هدى"))
    }

    @Test
    fun spaces_comeOneAtATime_andTatweelGoes() {
        assertEquals("قل هو لله حد", ArabicSearch.key("  قل   هو الله " + char(0x0640) + "احد "))
    }

    @Test
    fun onlyArabicLetters_countAsArabic() {
        assertTrue(ArabicSearch.hasArabic("الله"))
        assertFalse(ArabicSearch.hasArabic("mercy 2:255"))
        assertEquals("", ArabicSearch.key("mercy"))
    }
}
