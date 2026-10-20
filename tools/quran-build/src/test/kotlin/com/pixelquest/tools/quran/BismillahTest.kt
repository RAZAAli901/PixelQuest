package com.pixelquest.tools.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * The Bismillah rule on Tanzil's text: apart from the ayahs, above every surah but At-Tawbah, and an
 * ayah only in Al-Fatihah. The Bismillah's text is always taken from the file, never typed.
 */
class BismillahTest {

    private val arabic = ArabicSource.read(File("data/quran-source/quran-uthmani.xml"))
    private val opening = arabic.surahs.first().ayahs.first().text

    @Test
    fun tanzilsText_followsTheRule() {
        Bismillah.check(arabic)
    }

    @Test
    fun aboveEverySurah_butAlFatihahAndAtTawbah() {
        assertNull("Al-Fatihah's is its first ayah", arabic.surahs[0].bismillah)
        assertNull("At-Tawbah has none", arabic.surahs[8].bismillah)
        assertEquals(112, arabic.surahs.count { it.bismillah != null })
        assertFalse(Bismillah.shownAbove(1))
        assertFalse(Bismillah.shownAbove(9))
        assertTrue((2..114).filter { it != 9 }.all { Bismillah.shownAbove(it) })
    }

    @Test
    fun eachSurahKeepsItsOwn_asTanzilGivesIt() {
        // Al-Fatihah's first ayah, but for 95 and 97, whose Bismillah carries one more mark: a shadda,
        // as in their Mushaf. Nothing makes them alike.
        val differs = arabic.surahs.filter { it.bismillah != null && it.bismillah != opening }
        assertEquals(listOf(95, 97), differs.map { it.number })
        differs.forEach { surah ->
            val own = surah.bismillah!!
            assertEquals(opening.length + 1, own.length)
            val at = own.indices.first { it >= opening.length || own[it] != opening[it] }
            assertEquals('\u0651', own[at])
            assertEquals(opening, own.removeRange(at, at + 1))
        }
    }

    @Test
    fun itIsNeverPartOfAnotherSurahsFirstAyah() {
        // So nothing has to be cut out of any ayah: Al-Baqarah's first ayah doesn't start with it.
        arabic.surahs.drop(1).forEach { assertFalse("surah ${it.number}", it.ayahs.first().text.startsWith(opening)) }
    }

    @Test
    fun itCountsAsAnAyah_onlyInAlFatihah() {
        assertEquals(7, arabic.surahs[0].ayahs.size)
        assertEquals("Al-Fatihah's first ayah is the Bismillah", opening, arabic.surahs[0].ayahs[0].text)
    }

    @Test
    fun aMissingBismillah_stopsTheBuild() {
        val broken = arabic.copy(surahs = arabic.surahs.map { if (it.number == 2) it.copy(bismillah = null) else it })
        try {
            Bismillah.check(broken)
            fail("passed without Al-Baqarah's Bismillah")
        } catch (e: QuranBuildException) {
            assertTrue(e.message!!, "surah 2" in e.message!!)
        }
    }

    @Test
    fun aBismillahOnAtTawbah_stopsTheBuild() {
        val broken = arabic.copy(surahs = arabic.surahs.map { if (it.number == 9) it.copy(bismillah = opening) else it })
        try {
            Bismillah.check(broken)
            fail("passed with a Bismillah on At-Tawbah")
        } catch (e: QuranBuildException) {
            assertTrue(e.message!!, "surah 9" in e.message!!)
        }
    }
}
