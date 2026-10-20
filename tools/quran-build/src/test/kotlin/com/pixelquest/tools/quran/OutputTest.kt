package com.pixelquest.tools.quran

import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The app's two data files: the Arabic with each surah's details, and the translation, apart. Every
 * ayah is the source's, the notices go with them, and the files in the app are a fresh build's.
 */
class OutputTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val source = File("data/quran-source")
    private val json = Json { ignoreUnknownKeys = false }

    private fun build(): Pair<QuranFile, TranslationFile> {
        val out = temp.newFolder()
        QuranBuild(source, out).run()
        return json.decodeFromString<QuranFile>(File(out, Output.QURAN).readText()) to
            json.decodeFromString<TranslationFile>(File(out, Output.TRANSLATION).readText())
    }

    @Test
    fun theArabicFile_hasEverySurahsTextAndDetails() {
        val (quran, _) = build()
        val arabic = ArabicSource.read(File(source, "quran-uthmani.xml"))
        val metadata = MetadataSource.read(File(source, "quran-data.xml"))

        assertEquals(arabic.surahs.map { s -> s.ayahs.map { it.text } }, quran.surahs.map { it.ayahs })
        assertEquals(metadata.surahs.map { it.arabicName }, quran.surahs.map { it.arabicName })
        assertEquals(metadata.surahs.map { it.englishMeaning }, quran.surahs.map { it.englishMeaning })
        assertEquals(30, quran.juzs.size)
        assertNull(quran.surahs[0].bismillah)
        assertNull(quran.surahs[8].bismillah)
        assertEquals(arabic.surahs[1].bismillah, quran.surahs[1].bismillah)
    }

    @Test
    fun theTranslationFile_isApart_withItsCreditsAndTerms() {
        val (_, translation) = build()
        val text = TranslationSource.read(File(source, "en.sahih.txt"))

        assertEquals("en.sahih", translation.id)
        assertEquals("Saheeh International", translation.translator)
        translation.surahs.forEach { surah ->
            surah.ayahs.forEachIndexed { i, ayah -> assertEquals(text.ayahs[surah.number to i + 1], ayah) }
        }
        assertTrue("non-commercial" in translation.terms)
    }

    @Test
    fun bothFiles_carryTheirNotices() {
        val (quran, translation) = build()

        assertTrue(quran.notice.startsWith("# PLEASE DO NOT REMOVE OR CHANGE THIS COPYRIGHT BLOCK"))
        assertTrue("Tanzil.info" in quran.metadataNotice)
        assertTrue("Saheeh International" in translation.notice)
        assertTrue("https://tanzil.net" in quran.source)
    }

    @Test
    fun theVersion_followsTheSources_andIsTheSameEveryTime() {
        val (first, translation) = build()
        val (second, _) = build()

        assertEquals(first.version, second.version)
        assertEquals(first.version, translation.version)
        assertEquals(16, first.version.length)
    }

    @Test
    fun theAppsFiles_areAFreshBuildsExactly() {
        // So nothing in app/src/main/assets/quran is ever edited by hand: run the tool to change them.
        val out = temp.newFolder("fresh")
        QuranBuild(source, out).run()
        for (name in listOf(Output.QURAN, Output.TRANSLATION)) {
            assertArrayEquals(name, File(out, name).readBytes(), File("app/src/main/assets/quran", name).readBytes())
        }
    }
}
