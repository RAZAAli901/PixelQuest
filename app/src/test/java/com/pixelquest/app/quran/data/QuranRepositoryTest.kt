package com.pixelquest.app.quran.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.domain.Revelation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reader's repository on an in-memory database loaded from the real bundle: Al-Fatihah's first
 * ayah, An-Nas's last, the longest surah, pages, ranges and search, all offline. Every text is
 * compared with the bundled files, never typed in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuranRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val assets = AndroidQuranAssetSource(context)
    private val repository = QuranRepositoryImpl(db.quranDao())
    private val json = Json { ignoreUnknownKeys = true }
    private val quran = json.decodeFromString<QuranFile>(assets.read(QuranImporter.QURAN_FILE))
    private val translation = json.decodeFromString<TranslationFile>(assets.read(QuranImporter.TRANSLATION_FILE))

    @Before
    fun load() = runBlocking {
        assertFalse("nothing before the load", repository.isReady.first())
        QuranImporter(assets, db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
        Unit
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun afterTheLoad_itsReady() = runBlocking {
        assertTrue(repository.isReady.first())
    }

    @Test
    fun alFatihahsFirstAyah_isTheFilesOwn() = runBlocking {
        val first = repository.ayah(1, 1)!!

        assertEquals(1, first.id)
        assertEquals(quran.surahs[0].ayahs[0], first.arabic)
        assertEquals(translation.surahs[0].ayahs[0], first.translation)
        assertEquals("en.sahih", first.translationId)
    }

    @Test
    fun anNassLastAyah_isTheFilesOwn_andTheQuransLast() = runBlocking {
        val anNas = repository.surah(114)!!
        val last = repository.ayah(114, anNas.ayahCount)!!

        assertEquals(6236, last.id)
        assertEquals(quran.surahs.last().ayahs.last(), last.arabic)
        assertEquals(translation.surahs.last().ayahs.last(), last.translation)
        assertNull(repository.ayah(114, anNas.ayahCount + 1))
    }

    @Test
    fun theLongestSurah_isAlBaqarah_withAllItsAyahs() = runBlocking {
        val surahs = repository.surahs().first()
        val longest = surahs.maxBy { it.ayahCount }

        assertEquals(114, surahs.size)
        assertEquals(2, longest.number)
        assertEquals(quran.surahs[1].ayahs.size, longest.ayahCount)
        assertEquals(Revelation.MEDINAN, longest.revelation)
        assertEquals(quran.surahs[1].arabicName, longest.arabicName)
        assertEquals(quran.surahs[1].bismillah, longest.bismillah)
    }

    @Test
    fun aSurah_comesAPageAtATime_withoutGapsOrRepeats() = runBlocking {
        val pages = generateSequence(0) { it + 1 }
            .map { page -> runBlocking { repository.ayahs(2, page, pageSize = 50) } }
            .takeWhile { it.isNotEmpty() }
            .toList()

        assertEquals(6, pages.size)
        assertEquals(quran.surahs[1].ayahs, pages.flatten().map { it.arabic })
        assertEquals((1..quran.surahs[1].ayahs.size).toList(), pages.flatten().map { it.number })
    }

    @Test
    fun aRange_isTheAyahsAskedFor() = runBlocking {
        val range = repository.ayahRange(2, 255, 257)

        assertEquals(listOf(255, 256, 257), range.map { it.number })
        assertEquals(quran.surahs[1].ayahs.subList(254, 257), range.map { it.arabic })
    }

    @Test
    fun searchingTheTranslation_findsItsAyahs_inTheQuransOrder() = runBlocking {
        // A word of Al-Fatihah's first ayah, taken from the file.
        val word = translation.surahs[0].ayahs[0].split(" ").first { it.length > 5 }.trim(',', '.')
        val found = repository.searchTranslation(word)

        assertTrue(found.isNotEmpty())
        assertEquals(1, found.first().id)
        assertEquals(found.map { it.id }.sorted(), found.map { it.id })
        assertTrue(found.all { word.lowercase() in it.translation.lowercase() })
    }

    @Test
    fun aWordStillBeingTyped_matchesTheStartOfWords() = runBlocking {
        // Every match, whatever the screen's usual limit.
        val whole = repository.searchTranslation("Merciful", limit = 6236)
        val partial = repository.searchTranslation("Mercif", limit = 6236)

        assertTrue(whole.isNotEmpty())
        assertTrue("every ayah with the whole word has its start", partial.map { it.id }.containsAll(whole.map { it.id }))
        assertEquals("at most 200 by default", minOf(200, partial.size), repository.searchTranslation("Mercif").size)
    }

    @Test
    fun theJuz_startWhereTheFileSays_withTheirSurahsNames() = runBlocking {
        val juzs = repository.juzs().first()

        assertEquals(30, juzs.size)
        assertEquals(quran.juzs.map { it.surah to it.ayah }, juzs.map { it.surah to it.ayah })
        juzs.forEach { assertEquals(quran.surahs[it.surah - 1].englishName, it.surahName) }
    }

    @Test
    fun nothingToSearch_findsNothing() = runBlocking {
        assertTrue(repository.searchTranslation("  ?! ").isEmpty())
        assertTrue(repository.searchTranslation("zzqxj").isEmpty())
    }

    @Test
    fun theTranslation_comesWithItsCreditsAndTerms() = runBlocking {
        val only = repository.translations().first().single()

        assertEquals("Saheeh International", only.name)
        assertEquals("English", only.language)
        assertTrue("non-commercial" in only.licenceNote)
    }
}
