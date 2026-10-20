package com.pixelquest.tools.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * The build's checks: Tanzil's files pass them (114 surahs, 6,236 ayahs, each surah's count as the
 * metadata says, no empty text), and each kind of damage fails them, saying where.
 */
class ValidationTest {

    private val arabic = ArabicSource.read(File("data/quran-source/quran-uthmani.xml"))
    private val translation = TranslationSource.read(File("data/quran-source/en.sahih.txt"))
    private val metadata = MetadataSource.read(File("data/quran-source/quran-data.xml"))

    private fun refused(arabic: ArabicText = this.arabic, translation: TranslationText = this.translation, metadata: Metadata = this.metadata): String {
        try {
            Validation.check(arabic, translation, metadata)
        } catch (e: QuranBuildException) {
            return e.message!!
        }
        fail("passed")
        error("unreachable")
    }

    private fun ArabicText.withSurah(number: Int, change: (SourceSurah) -> SourceSurah) =
        copy(surahs = surahs.map { if (it.number == number) change(it) else it })

    @Test
    fun tanzilsFiles_pass() {
        Validation.check(arabic, translation, metadata)
        assertEquals(Validation.SURAHS, arabic.surahs.size)
        assertEquals(Validation.AYAHS, arabic.surahs.sumOf { it.ayahs.size })
    }

    @Test
    fun aMissingSurah_fails() {
        assertTrue(refused(arabic = arabic.copy(surahs = arabic.surahs.dropLast(1))).contains("113 surahs"))
    }

    @Test
    fun aMissingAyah_fails_atItsSurah() {
        val message = refused(arabic = arabic.withSurah(2) { it.copy(ayahs = it.ayahs.dropLast(1)) })
        assertTrue(message, "6235 ayahs" in message)
    }

    @Test
    fun aSurahLongerThanTheMetadataSays_fails() {
        // One ayah moved from 3 to 2: the total holds, but both surahs disagree with the metadata.
        val moved = arabic.withSurah(2) { it.copy(ayahs = it.ayahs + SourceAyah(287, "x")) }
            .withSurah(3) { it.copy(ayahs = it.ayahs.dropLast(1)) }
        val message = refused(arabic = moved)
        assertTrue(message, "surah 2 has 287 ayahs" in message)
    }

    @Test
    fun ayahsOutOfOrder_fail() {
        val message = refused(arabic = arabic.withSurah(1) { it.copy(ayahs = it.ayahs.reversed()) })
        assertTrue(message, "surah 1's ayahs" in message)
    }

    @Test
    fun anEmptyAyah_fails_namingIt() {
        val message = refused(arabic = arabic.withSurah(112) { s -> s.copy(ayahs = s.ayahs.map { if (it.number == 3) it.copy(text = " ") else it }) })
        assertTrue(message, "112:3 has no text" in message)
    }

    @Test
    fun anEmptyTranslation_fails_namingIt() {
        val message = refused(translation = translation.copy(ayahs = translation.ayahs + ((2 to 255) to "")))
        assertTrue(message, "2:255 has no text" in message)
    }

    @Test
    fun aShortTranslation_fails() {
        val message = refused(translation = translation.copy(ayahs = translation.ayahs - (114 to 6)))
        assertTrue(message, "6235 ayahs" in message)
    }
}
