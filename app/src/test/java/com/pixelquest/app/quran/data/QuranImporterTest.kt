package com.pixelquest.app.quran.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Loading the Quran from the app's real bundled files: every ayah reaches the database exactly as
 * the files hold it, in one transaction. Compared with the files themselves, never typed-in text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuranImporterTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val assets = AndroidQuranAssetSource(context)
    private val importer = QuranImporter(assets, db.quranDao(), RoomTransactionRunner(db))
    private val json = Json { ignoreUnknownKeys = true }
    private val quran = json.decodeFromString<QuranFile>(assets.read(QuranImporter.QURAN_FILE))
    private val translation = json.decodeFromString<TranslationFile>(assets.read(QuranImporter.TRANSLATION_FILE))

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun theWholeQuran_isLoaded_exactlyAsTheFilesHoldIt() = runBlocking {
        val result = importer.import()

        assertEquals(QuranImport(quran.version, 114, 6236), result)
        val dao = db.quranDao()
        assertEquals(114, dao.surahCount())
        assertEquals(6236, dao.ayahCount())
        var id = 0
        for ((surah, translated) in quran.surahs.zip(translation.surahs)) {
            val rows = dao.ayahsOf(surah.number, offset = 0, limit = 1000)
            assertEquals(surah.ayahs, rows.map { it.arabic })
            assertEquals(translated.ayahs, rows.map { it.translation })
            assertEquals((1..surah.ayahs.size).toList(), rows.map { it.ayah })
            assertEquals("each ayah's id is its place in the Quran", (id + 1..id + surah.ayahs.size).toList(), rows.map { it.id })
            id += surah.ayahs.size
        }
    }

    @Test
    fun eachSurah_hasItsDetails_andItsOwnBismillah() = runBlocking {
        importer.import()

        val dao = db.quranDao()
        quran.surahs.forEach { file ->
            val row = dao.getSurah(file.number)!!
            assertEquals(file.arabicName, row.arabicName)
            assertEquals(file.englishMeaning, row.englishMeaning)
            assertEquals(file.ayahs.size, row.ayahCount)
            assertEquals(file.bismillah, row.bismillah)
        }
        assertNull(dao.getSurah(1)!!.bismillah)
        assertNull(dao.getSurah(9)!!.bismillah)
    }

    @Test
    fun theTranslationsDetails_markTheLoadDone() = runBlocking {
        importer.import()

        val info = db.quranDao().getTranslation("en.sahih")!!
        assertEquals("Saheeh International", info.translator)
        assertEquals(quran.version, info.dataVersion)
        assertTrue("non-commercial" in info.licenceNote)
        assertTrue("Saheeh International" in info.notice)
    }

    @Test
    fun aDamagedBundle_writesNothing() = runBlocking {
        // A translation file one ayah short: the Arabic would be loaded without it.
        val short = translation.copy(surahs = translation.surahs.map { if (it.number == 2) it.copy(ayahs = it.ayahs.dropLast(1)) else it })
        val damaged = QuranAssetSource { name ->
            if (name == QuranImporter.TRANSLATION_FILE) Json.encodeToString(TranslationFile.serializer(), short) else assets.read(name)
        }

        try {
            QuranImporter(damaged, db.quranDao(), RoomTransactionRunner(db)).import()
            fail("loaded a damaged bundle")
        } catch (e: QuranImportException) {
            assertTrue(e.message!!, "don't match" in e.message!!)
        }
        assertEquals(0, db.quranDao().surahCount())
        assertEquals(0, db.quranDao().ayahCount())
    }

    @Test
    fun ifNeeded_loadsOnce_thenNotAgain() = runBlocking {
        assertEquals(QuranImport(quran.version, 114, 6236), importer.importIfNeeded())

        assertNull("the phone holds this bundle", importer.importIfNeeded())
        assertEquals(6236, db.quranDao().ayahCount())
    }

    @Test
    fun aNewerBundle_replacesTheOld() = runBlocking {
        importer.import()
        val newer = QuranAssetSource { name ->
            when (name) {
                QuranImporter.QURAN_FILE -> Json.encodeToString(QuranFile.serializer(), quran.copy(version = "0123456789abcdef"))
                else -> Json.encodeToString(TranslationFile.serializer(), translation.copy(version = "0123456789abcdef"))
            }
        }

        val result = QuranImporter(newer, db.quranDao(), RoomTransactionRunner(db)).importIfNeeded()

        assertEquals("0123456789abcdef", result!!.version)
        assertEquals("0123456789abcdef", db.quranDao().getTranslation("en.sahih")!!.dataVersion)
        assertEquals(6236, db.quranDao().ayahCount())
    }

    @Test
    fun anIncompleteCopy_isLoadedAgain() = runBlocking {
        importer.import()
        // As if the phone's copy lost its ayahs: the version alone doesn't make it complete.
        db.quranDao().deleteAyahs()

        assertEquals(6236, importer.importIfNeeded()!!.ayahs)
        assertEquals(6236, db.quranDao().ayahCount())
    }

    @Test
    fun theBundlesVersion_isReadFromItsStart() {
        assertTrue(assets.head(QuranImporter.TRANSLATION_FILE, 200).contains("\"version\":\"${quran.version}\""))
    }

    @Test
    fun loadingAgain_replacesRatherThanDoubles() = runBlocking {
        importer.import()
        importer.import()

        assertEquals(114, db.quranDao().surahCount())
        assertEquals(6236, db.quranDao().ayahCount())
    }
}
