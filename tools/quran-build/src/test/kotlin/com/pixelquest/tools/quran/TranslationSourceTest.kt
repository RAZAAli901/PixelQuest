package com.pixelquest.tools.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Saheeh International as the parser reads it: the file's own text for every ayah, its credits, and
 * joined to the Arabic ayah by ayah. Checked against the file itself, not typed-in text.
 */
class TranslationSourceTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val file = File("data/quran-source/en.sahih.txt")
    private val translation = TranslationSource.read(file)

    @Test
    fun everyAyah_isTheLinesOwnText_exactly() {
        val lines = file.readText(Charsets.UTF_8).split("\n").filter { it.isNotEmpty() && !it.startsWith("#") }

        assertEquals(6236, translation.ayahs.size)
        for (line in lines) {
            val (surah, ayah) = line.split("|").take(2).map { it.toInt() }
            assertEquals(line.substring(line.indexOf('|', line.indexOf('|') + 1) + 1), translation.ayahs[surah to ayah])
        }
    }

    @Test
    fun itsCredits_comeFromItsNotice() {
        assertEquals("en.sahih", translation.id)
        assertEquals("Saheeh International", translation.name)
        assertEquals("Saheeh International", translation.translator)
        assertEquals("English", translation.language)
        assertEquals("April 24, 2011", translation.lastUpdate)
        assertTrue(translation.notice.lines().all { it.startsWith("#") })
    }

    @Test
    fun joined_everyArabicAyahHasItsTranslation() {
        val arabic = ArabicSource.read(File("data/quran-source/quran-uthmani.xml"))

        val joined = Join.of(arabic, translation)

        assertEquals(6236, joined.sumOf { it.ayahs.size })
        joined.forEach { surah ->
            surah.ayahs.forEach { ayah -> assertEquals(translation.ayahs[surah.number to ayah.number], ayah.translation) }
        }
        assertEquals(arabic.surahs.flatMap { it.ayahs }.map { it.text }, joined.flatMap { it.ayahs }.map { it.arabic })
    }

    @Test
    fun aMissingTranslatedAyah_stopsTheBuild() {
        val arabic = ArabicSource.read(File("data/quran-source/quran-uthmani.xml"))
        val short = translation.copy(ayahs = translation.ayahs - (2 to 255))

        try {
            Join.of(arabic, short)
            fail("joined without 2:255")
        } catch (e: QuranBuildException) {
            assertTrue(e.message!!, "2:255" in e.message!!)
        }
    }

    @Test
    fun aLineThatIsntSurahAyahText_stopsTheBuild() {
        val bad = temp.newFile("bad.txt").apply { writeText("1|1|fine\nnot a line\n# ID: x\n") }
        try {
            TranslationSource.read(bad)
            fail("read a bad line")
        } catch (e: QuranBuildException) {
            assertTrue(e.message!!, "not surah|ayah|text" in e.message!!)
        }
    }
}
