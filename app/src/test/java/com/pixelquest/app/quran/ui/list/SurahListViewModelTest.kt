package com.pixelquest.app.quran.ui.list

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranImporter
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The surah list on the real 114 surahs: all of them, then those a search finds as it's typed. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SurahListViewModelTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val viewModels = TestViewModels()
    private val lastRead = com.pixelquest.app.quran.data.MemoryLastReadStore()
    private lateinit var vm: SurahListViewModel

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
        vm = viewModels.keep(SurahListViewModel::class.java) { SurahListViewModel(QuranRepositoryImpl(db.quranDao()), lastRead) }
        launch(Dispatchers.Unconfined) { vm.state.first { it.loaded } }.join()
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    private fun found(query: String): List<Int> = runBlocking {
        vm.search(query)
        vm.state.first { it.query == query && it.loaded }.surahs.map { it.number }
    }

    @Test
    fun withNoSearch_all114_inOrder() {
        assertEquals((1..114).toList(), found(""))
        assertEquals((1..114).toList(), found("   "))
    }

    @Test
    fun aNumber_findsTheSurahsStartingWithIt() {
        assertEquals(listOf(2) + (20..29), found("2"))
        assertEquals(listOf(11, 110, 111, 112, 113, 114), found("11"))
        assertEquals(listOf(114), found("114"))
        assertTrue(found("115").isEmpty())
    }

    @Test
    fun arabicIndicDigits_countAsNumbers() {
        assertEquals(listOf(36), found("٣٦"))
    }

    @Test
    fun anEnglishName_spelledNearly_findsItsSurah() {
        assertEquals(listOf(1), found("Fatiha"))
        assertEquals(listOf(1), found("al faatiha"))
        assertEquals(listOf(2), found("baqara"))
    }

    @Test
    fun aMeaning_findsItsSurah() {
        assertEquals(listOf(2), found("cow"))
        assertTrue(1 in found("opening"))
    }

    @Test
    fun anArabicName_findsItsSurah_withOrWithoutMarks() {
        val alBaqarah = runBlocking { QuranRepositoryImpl(db.quranDao()).surah(2)!!.arabicName }
        assertEquals(listOf(2), found(alBaqarah))
        assertEquals(listOf(2), found(SurahSearch.arabic(alBaqarah).drop(2)))
    }

    @Test
    fun theLastPlaceRead_comesWithItsSurah_andNoneBeforeAnyReading() = runBlocking {
        assertEquals(null, vm.state.value.continueReading)

        lastRead.save(com.pixelquest.app.quran.data.LastRead(2, 255))
        val place = vm.state.first { it.continueReading != null }.continueReading!!

        assertEquals("Al-Baqara", place.surah.englishName)
        assertEquals(255, place.ayah)
    }

    @Test
    fun aPlacePastItsSurahsEnd_isItsLastAyah() = runBlocking {
        lastRead.save(com.pixelquest.app.quran.data.LastRead(1, 99))

        assertEquals(7, vm.state.first { it.continueReading != null }.continueReading!!.ayah)
    }

    @Test
    fun nonsense_findsNothing() {
        assertTrue(found("zzqx").isEmpty())
        assertFalse(found("?!").isNotEmpty())
    }
}
