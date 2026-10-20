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
        override fun translations(): Flow<List<Translation>> = flowOf(emptyList())
    }

    private val repository = TwoSurahs()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun reader(surah: Int, ayah: Int? = null, saved: SavedStateHandle? = null) =
        SurahReaderViewModel(saved ?: SavedStateHandle(mapOf("surah" to surah, "ayah" to ayah).filterValues { it != null }), repository)

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
    fun afterARotation_itComesBackWhereThePlayerWas() {
        val saved = SavedStateHandle(mapOf("surah" to 2))
        reader(2, saved = saved).apply {
            onScrolled(120)
            setDisplayMode(DisplayMode.ARABIC)
        }

        val again = reader(2, saved = saved)

        assertEquals(120, again.state.value.target)
        assertEquals(DisplayMode.ARABIC, again.state.value.displayMode)
        assertTrue(again.state.value.ayahs.size >= 120)
    }
}
