package com.pixelquest.app.quran.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Searching the real Uthmani text with everyday Arabic, and the loader filling the key after the update. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ArabicSearchOnTheQuranTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repository = QuranRepositoryImpl(db.quranDao())
    private val importer = QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }

    @Before
    fun setUp() = runBlocking {
        importer.import()
        Unit
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun found(text: String) = runBlocking { repository.searchArabic(text) }.map { it.surah to it.number }

    @Test
    fun theBismillah_typedPlain_findsTheTwoAyahsThatHoldIt() {
        assertEquals(listOf(1 to 1, 27 to 30), found("بسم الله الرحمن الرحيم"))
    }

    @Test
    fun everydaySpelling_findsTheUthmaniText() {
        assertTrue(2 to 2 in found("ذلك الكتاب لا ريب فيه"))
        assertEquals(listOf(112 to 1), found("قل هو الله احد"))
    }

    @Test
    fun typedWithMarks_findsTheSame() {
        assertEquals(found("قل هو الله احد"), found("قُلْ هُوَ اللَّهُ أَحَدٌ"))
    }

    @Test
    fun theTextShown_isStillTheSourcesOwn() = runBlocking {
        val ayah = repository.ayah(112, 1)!!
        val stored = db.quranDao().getAyah(112, 1)!!

        assertEquals(stored.arabic, ayah.arabic)
        assertTrue("the key is beside the text, not in place of it", stored.arabicPlain != stored.arabic)
    }

    @Test
    fun aDatabaseWithoutKeys_isLoadedAgain() = runBlocking {
        assertNull("complete: nothing to load", importer.importIfNeeded())
        db.openHelper.writableDatabase.execSQL("UPDATE quran_ayah SET arabicPlain = ''")

        val again = importer.importIfNeeded()

        assertEquals(QuranImporter.AYAHS, again!!.ayahs)
        assertEquals(0, db.quranDao().ayahCountWithoutPlain())
    }
}
