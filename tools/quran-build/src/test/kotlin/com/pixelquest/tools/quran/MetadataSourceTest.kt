package com.pixelquest.tools.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Tanzil's metadata as the parser reads it: checked against the file itself, read a second way. */
class MetadataSourceTest {

    private val file = File("data/quran-source/quran-data.xml")
    private val metadata = MetadataSource.read(file)

    /** Each `<sura …/>` element's attributes, read as plain text. */
    private fun rawSuras(): List<Map<String, String>> =
        Regex("<sura (index=[^>]*)/>").findAll(file.readText(Charsets.UTF_8)).map { m ->
            Regex("(\\w+)=\"([^\"]*)\"").findAll(m.groupValues[1]).associate { it.groupValues[1] to it.groupValues[2] }
        }.toList()

    @Test
    fun everySurah_hasTheFilesNames_place_andCount() {
        val raw = rawSuras()

        assertEquals(114, metadata.surahs.size)
        assertEquals(raw.size, metadata.surahs.size)
        metadata.surahs.zip(raw).forEach { (info, attrs) ->
            assertEquals(attrs["index"]!!.toInt(), info.number)
            assertEquals(attrs["name"], info.arabicName)
            assertEquals(attrs["tname"], info.englishName)
            assertEquals(attrs["ename"], info.englishMeaning)
            assertEquals(attrs["type"], info.revelation)
            assertEquals(attrs["ayas"]!!.toInt(), info.ayahCount)
            assertEquals(attrs["order"]!!.toInt(), info.revelationOrder)
        }
        assertTrue(metadata.surahs.all { it.revelation == "Meccan" || it.revelation == "Medinan" })
    }

    @Test
    fun thirtyJuz_fromTheFirstAyah_inOrder() {
        assertEquals((1..30).toList(), metadata.juzs.map { it.number })
        assertEquals(JuzStart(1, 1, 1), metadata.juzs.first())
        val starts = metadata.juzs.map { it.surah * 1000 + it.ayah }
        assertEquals(starts.sorted(), starts)
    }

    @Test
    fun eachSurah_knowsTheJuzItStartsIn() {
        assertEquals(1, metadata.surahs.first().juz)
        assertEquals(30, metadata.surahs.last().juz)
        val juzs = metadata.surahs.map { it.juz }
        assertEquals("never going back a juz", juzs.sorted(), juzs)
        // A juz that starts on a surah's first ayah is that surah's.
        metadata.juzs.filter { it.ayah == 1 }.forEach { assertEquals(it.number, metadata.surahs[it.surah - 1].juz) }
    }

    @Test
    fun aJuz_isTheLastToStartAtOrBefore() {
        val juzs = metadata.juzs
        val second = juzs[1]
        assertEquals(2, MetadataSource.juzOf(second.surah, second.ayah, juzs))
        assertEquals(1, MetadataSource.juzOf(second.surah, second.ayah - 1, juzs))
    }

    @Test
    fun theNotice_keepsItsCopyrightAndLicence() {
        assertTrue(metadata.notice, "(C) 2008-2009 Tanzil.info" in metadata.notice)
        assertTrue(metadata.notice, "cc-by" in metadata.notice)
    }
}
