package com.pixelquest.app.ui.screens.leveling

import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.domain.repository.LevelHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Level History used to start from an empty list, so it flashed "no level-ups yet" while loading. */
class LevelHistoryLoadingTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theHistory_isNullUntilLoaded_thenTheList() = runTest {
        val source = MutableSharedFlow<List<LevelHistoryEntity>>()
        val repository = object : LevelHistoryRepository {
            override suspend fun insertLevelHistory(entry: LevelHistoryEntity) = Unit
            override fun getAllHistory(): Flow<List<LevelHistoryEntity>> = source
        }
        val viewModel = LevelHistoryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.history.collect {} }

        assertNull("Not loaded yet", viewModel.history.value)

        source.emit(emptyList())
        assertEquals("Loaded, and genuinely empty", emptyList<LevelHistoryEntity>(), viewModel.history.value)
    }
}
