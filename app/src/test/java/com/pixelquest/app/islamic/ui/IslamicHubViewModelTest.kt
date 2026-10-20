package com.pixelquest.app.islamic.ui

import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.IslamicItemProvider
import com.pixelquest.app.islamic.IslamicRegistry
import com.pixelquest.app.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The hub's state: the items that are on, in order, with their badges, and Simple Mode. */
@OptIn(ExperimentalCoroutinesApi::class)
class IslamicHubViewModelTest {

    private class Item(override val order: Int, id: String) : IslamicItemProvider {
        val state = MutableStateFlow(IslamicItem(id, R.string.menu_settings, R.string.menu_settings, IslamicIcon("•"), "islamic/$id"))
        override fun item(): Flow<IslamicItem> = state
    }

    private val namaz = Item(10, "namaz")
    private val darood = Item(20, "darood")
    private val quran = Item(30, "quran")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun beforeTheRegistryAnswers_itIsNotLoaded() {
        assertFalse(IslamicHubViewModel(IslamicRegistry(setOf(namaz)), FakeSettingsRepository()).state.value.loaded)
    }

    @Test
    fun theItemsThatAreOn_inOrder_withTheirBadges() = runTest {
        val vm = IslamicHubViewModel(IslamicRegistry(setOf(quran, darood, namaz)), FakeSettingsRepository())
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        assertTrue(vm.state.value.loaded)
        assertEquals(listOf("namaz", "darood", "quran"), vm.state.value.items.map { it.id })

        darood.state.value = darood.state.value.copy(enabled = false)
        namaz.state.value = namaz.state.value.copy(badgeCount = 3)
        advanceUntilIdle()

        assertEquals("a switched-off item leaves the hub", listOf("namaz", "quran"), vm.state.value.items.map { it.id })
        assertEquals(3, vm.state.value.items.first().badgeCount)
    }

    @Test
    fun simpleMode_reachesTheHub() = runTest {
        val vm = IslamicHubViewModel(IslamicRegistry(setOf(namaz)), FakeSettingsRepository(simpleMode = true))
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        assertTrue(vm.state.value.isSimpleMode)
    }
}
