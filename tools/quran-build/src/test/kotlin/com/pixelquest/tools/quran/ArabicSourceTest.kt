package com.pixelquest.tools.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Arabic as the parser reads it is the file's, character for character. Checked against a
 * second, plain reading of the same file, so no ayah is ever typed into a test.
 */
class ArabicSourceTest {

    private val file = File("data/quran-source/quran-uthmani.xml")
    private val arabic = ArabicSource.read(file)

    /** Every `<aya index="…" text="…"` in the file, read as plain text, in order. */
    private fun rawAyahs(): List<Pair<Int, String>> {
        val raw = file.readText(Charsets.UTF_8)
        return Regex("<aya index=\"(\\d+)\" text=\"([^\"]*)\"").findAll(raw).map { it.groupValues[1].toInt() to it.groupValues[2] }.toList()
    }

    @Test
    fun everyAyah_isTheFilesOwnText_characterForCharacter() {
        val raw = rawAyahs()
        val parsed = arabic.surahs.flatMap { it.ayahs }.map { it.number to it.text }

        assertEquals(raw.size, parsed.size)
        assertEquals(raw, parsed)
    }

    @Test
    fun theTextHasNoEscapes_soThePlainReadingIsTheWholeTruth() {
        // An &amp; or the like would make the plain reading above differ from the XML's meaning.
        assertFalse(rawAyahs().any { '&' in it.second })
    }

    @Test
    fun theSurahs_comeNumbered_inTheFilesOrder() {
        assertEquals((1..114).toList(), arabic.surahs.map { it.number })
        assertEquals((1..286).toList(), arabic.surahs[1].ayahs.map { it.number })
    }

    @Test
    fun theNotice_isTanzilsCopyrightBlock_whole() {
        assertTrue(arabic.notice.startsWith("# PLEASE DO NOT REMOVE OR CHANGE THIS COPYRIGHT BLOCK"))
        assertTrue("Tanzil Quran Text (Uthmani, Version 1.1)" in arabic.notice)
        assertTrue("CHANGING IT IS NOT ALLOWED" in arabic.notice)
        assertTrue(arabic.notice.endsWith("#===================================================================="))
        // The same block licenses/ quotes.
        assertTrue(File("licenses/tanzil-quran-text.txt").readText().replace("\r\n", "\n").contains(arabic.notice))
    }
}
