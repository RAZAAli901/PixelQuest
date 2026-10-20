package com.pixelquest.app.quran.ui.reader

import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Juz
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.domain.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

/** The reader's state: pages of ayahs as the player scrolls, opening at an ayah, and keeping its place. */
@OptIn(ExperimentalCoroutinesApi::class)
class SurahReaderViewModelTest {

    /** A Quran of two surahs: 286 ayahs and 7, their text made up ("2:1" and so on), not the Quran's. */
    private class TwoSurahs : QuranRepository {
        val pagesAsked = mutableListOf<Int>()
        private val counts = mapOf(2 to 286, 1 to 7)
        override val isReady: Flow<Boolean> = flowOf(true)
        override fun surahs(): Flow<List<Surah>> = flowOf(emptyList())
        override suspend fun surah(number: Int) =
            counts[number]?.let { Surah(number, "ـ", "S$number", "M$number", Revelation.MEDINAN, it, 1, null) }
        override fun juzs(): Flow<List<Juz>> = flowOf(emptyList())
        override suspend fun ayahs(surah: Int, page: Int, pageSize: Int): List<Ayah> {
            pagesAsked += page
            val count = counts[surah] ?: 0
            return (page * pageSize + 1..minOf(count, (page + 1) * pageSize)).map { Ayah(it, surah, it, "$surah:$it", "t$it", "en.test") }
        }
        override suspend fun ayahRange(surah: Int, from: Int, to: Int) = emptyList<Ayah>()
        override suspend fun ayah(surah: Int, number: Int): Ayah? = null
        override suspend fun searchTranslation(text: String, limit: Int) = emptyList<Ayah>()
        override suspend fun searchArabic(text: String, limit: Int) = emptyList<Ayah>()
        override fun translations(): Flow<List<Translation>> = flowOf(emptyList())
    }

    private val repository = TwoSurahs()
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val settings = com.pixelquest.app.quran.data.MemoryReadingSettingsStore()
    private val bookmarks = com.pixelquest.app.quran.data.MemoryBookmarks()
    private val lastRead = com.pixelquest.app.quran.data.MemoryLastReadStore()

    private fun reader(surah: Int, ayah: Int? = null, saved: SavedStateHandle? = null) =
        SurahReaderViewModel(saved ?: SavedStateHandle(mapOf("surah" to surah, "ayah" to ayah).filterValues { it != null }), repository, settings, bookmarks, lastRead)

    @Test
    fun aSurah_opensAtItsStart_withItsFirstPage() {
        val vm = reader(2)

        val state = vm.state.value
        assertEquals(2, state.surah!!.number)
        assertEquals((1..50).toList(), state.ayahs.map { it.number })
        assertTrue(state.hasMore)
        assertNull(state.target)
        assertEquals(DisplayMode.BOTH, state.displayMode)
    }

    @Test
    fun scrollingOn_loadsPageByPage_toItsEnd() {
        val vm = reader(2)

        repeat(10) { vm.loadMore() }

        assertEquals((1..286).toList(), vm.state.value.ayahs.map { it.number })
        assertFalse(vm.state.value.hasMore)
        assertEquals("no page asked for past the end", (0..5).toList(), repository.pagesAsked)
    }

    @Test
    fun itKnowsItsNeighbours_andTheEndsHaveOne() {
        val middle = reader(2)
        assertEquals(1, middle.state.value.previous!!.number)
        assertNull("this made-up Quran ends at 2", middle.state.value.next)

        val first = reader(1)
        assertNull(first.state.value.previous)
        assertEquals(2, first.state.value.next!!.number)
    }

    @Test
    fun aShortSurah_isAllThere_inOnePage() {
        val vm = reader(1)

        assertEquals(7, vm.state.value.ayahs.size)
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun openedAtAnAyah_itLoadsUpToIt_andAimsThere() {
        val vm = reader(2, ayah = 255)

        assertTrue(vm.state.value.ayahs.size >= 255)
        assertEquals(255, vm.state.value.target)
        vm.onTargetShown()
        assertNull(vm.state.value.target)
    }

    @Test
    fun openedAtAnAyah_itFlashesIt_untilShown() {
        val vm = reader(2, ayah = 255)
        assertEquals(255, vm.state.value.flash)

        vm.onFlashShown()

        assertNull(vm.state.value.flash)
        assertEquals("the first ayah too", 1, reader(2, ayah = 1).state.value.flash)
        assertNull("opened at its start, nothing flashes", reader(2, ayah = 0).state.value.flash)
    }

    @Test
    fun afterARotation_itComesBackWhereThePlayerWas() {
        val saved = SavedStateHandle(mapOf("surah" to 2))
        reader(2, saved = saved).onScrolled(120)

        val again = reader(2, saved = saved)

        assertEquals(120, again.state.value.target)
        assertTrue(again.state.value.ayahs.size >= 120)
        assertNull("nothing flashes after a rotation", again.state.value.flash)
    }

    @Test
    fun aBookmark_togglesOnAndOff_forThisSurahsAyah() {
        val vm = reader(2)

        vm.toggleBookmark(255)
        assertEquals(setOf(255), vm.state.value.bookmarked)
        assertEquals(listOf(2 to 255), bookmarks.marks.value)

        vm.toggleBookmark(255)
        assertTrue(vm.state.value.bookmarked.isEmpty())
    }

    @Test
    fun wherePlayerSettles_isKeptAsTheLastPlaceRead() {
        val vm = reader(2)

        vm.onScrolled(120)
        vm.onScrolled(121)
        assertNull("not at every ayah a fling passes", lastRead.lastRead.value)

        main.scheduler.advanceTimeBy(SurahReaderViewModel.KEEP_PLACE_AFTER_MS + 1)
        assertEquals(com.pixelquest.app.quran.data.LastRead(2, 121), lastRead.lastRead.value)
    }

    @Test
    fun theSettings_areKept_forTheNextVisit() {
        reader(2).setDisplayMode(DisplayMode.ARABIC)

        val nextVisit = reader(1)

        assertEquals(DisplayMode.ARABIC, nextVisit.state.value.displayMode)
    }
}
