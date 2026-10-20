package com.pixelquest.app.quran.ui.search

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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

/** Searching the real Quran: surahs by name and number, an ayah by reference, ayahs by their translation. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuranSearchViewModelTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val viewModels = TestViewModels()
    private lateinit var vm: QuranSearchViewModel

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
        vm = viewModels.keep(QuranSearchViewModel::class.java) { QuranSearchViewModel(QuranRepositoryImpl(db.quranDao())) }
        launch(Dispatchers.Unconfined) { vm.state.first() }.join()
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    private fun search(query: String): QuranSearchUiState = runBlocking {
        vm.search(query)
        vm.state.first { it.query == query }
    }

    @Test
    fun aSurahsName_findsTheSurah() {
        val found = search("baqara")

        assertEquals(listOf(2), found.surahs.map { it.number })
    }

    @Test
    fun words_findAyahsByTheirTranslation_inTheQuransOrder_withTheirSurahsName() {
        val found = search("throne")

        assertTrue(found.ayahs.size > 5)
        assertTrue(found.ayahs.all { "throne" in it.ayah.translation.lowercase() })
        assertEquals(found.ayahs.map { it.ayah.id }.sorted(), found.ayahs.map { it.ayah.id })
        val first = found.ayahs.first()
        assertEquals(runBlocking { QuranRepositoryImpl(db.quranDao()).surah(first.ayah.surah)!!.englishName }, first.surahName)
    }

    @Test
    fun aReference_findsThatAyahFirst() {
        val found = search("2:255")

        assertEquals(2 to 255, found.ayahs.first().ayah.let { it.surah to it.number })
        assertNull(QuranSearchViewModel.reference("2:"))
        assertEquals(36 to 1, QuranSearchViewModel.reference(" 36.1 "))
    }

    @Test
    fun aCommonWord_isCapped_atTheSearchLimit() {
        val found = search("allah")

        assertEquals(QuranRepository.SEARCH_LIMIT, found.ayahs.size)
        assertTrue(found.capped)
        assertFalse(search("throne").capped)
    }

    @Test
    fun arabicTyped_searchesTheArabic() {
        val found = search("قل هو الله احد")

        assertTrue(found.arabic)
        assertEquals(listOf(112 to 1), found.ayahs.map { it.ayah.surah to it.ayah.number })
        assertFalse(search("throne").arabic)
    }

    @Test
    fun nothingTyped_findsNothing() {
        assertTrue(search("  ").isEmpty)
    }
}
