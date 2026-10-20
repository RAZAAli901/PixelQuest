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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The translation switcher: bundled translations are found by their files, the player's choice is
 * kept, and the loader puts the chosen one in every ayah. A second translation is made here from
 * the real one's file with a new id, standing in for a file added later.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TranslationSwitcherTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val real = AndroidQuranAssetSource(context)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    private class MemoryChoice : TranslationChoiceStore {
        var id: String? = null
        override fun chosen() = id
        override fun choose(id: String) { this.id = id }
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** The real files, plus `quran_xx.json`: Saheeh International's text under another id and name. */
    private val withASecond = object : QuranAssetSource {
        private val json = Json { ignoreUnknownKeys = true }
        private val second by lazy {
            val en = json.decodeFromString<TranslationFile>(real.read(QuranImporter.TRANSLATION_FILE))
            Json.encodeToString(TranslationFile.serializer(), en.copy(id = "xx.test", name = "Test", language = "Test"))
        }
        override fun read(name: String) = if (name == "quran_xx.json") second else real.read(name)
        override fun list() = real.list() + "quran_xx.json"
    }

    @Test
    fun theBundledTranslation_isFoundByItsFile() {
        val available = TranslationSwitcher(real, MemoryChoice()).available()

        assertEquals(listOf(BundledTranslation("en.sahih", "Saheeh International", "Saheeh International", "English", "quran_en.json")), available)
    }

    @Test
    fun untilOneIsChosen_SaheehInternationalIsRead() {
        val switcher = TranslationSwitcher(withASecond, MemoryChoice())

        assertEquals(2, switcher.available().size)
        assertEquals("en.sahih", switcher.selected().id)
    }

    @Test
    fun aChoice_isKept_onlyIfItsBundled() {
        val choice = MemoryChoice()
        val switcher = TranslationSwitcher(withASecond, choice)

        assertTrue(switcher.select("xx.test"))
        assertEquals("xx.test", switcher.selected().id)
        assertFalse(switcher.select("fr.hamidullah"))
        assertEquals("xx.test", choice.id)
    }

    @Test
    fun aChoiceNoLongerBundled_fallsBackToTheDefault() {
        val choice = MemoryChoice().apply { id = "gone.away" }

        assertEquals("en.sahih", TranslationSwitcher(real, choice).selected().id)
    }

    @Test
    fun switching_loadsTheChosenTranslation_intoEveryAyah() = runBlocking {
        val choice = MemoryChoice()
        val switcher = TranslationSwitcher(withASecond, choice)
        val importer = QuranImporter(withASecond, db.quranDao(), RoomTransactionRunner(db), switcher)
        importer.importIfNeeded()
        assertEquals(6236, db.quranDao().ayahCountIn("en.sahih"))

        switcher.select("xx.test")
        assertEquals(6236, importer.importIfNeeded()!!.ayahs)

        assertEquals(6236, db.quranDao().ayahCountIn("xx.test"))
        assertEquals(0, db.quranDao().ayahCountIn("en.sahih"))
        assertNull("nothing more to load", importer.importIfNeeded())
    }
}
