package com.pixelquest.app.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {

    private val standardTestDispatcher = StandardTestDispatcher()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(standardTestDispatcher)

    @Test
    fun mainDispatcherRule_overridesMainDispatcherDuringTestExecution() = runTest(standardTestDispatcher) {
        var executed = false
        launch(Dispatchers.Main) {
            executed = true
        }

        // Initially un-advanced
        assertTrue(!executed)

        advanceUntilIdle()
        assertTrue(executed)
    }

    @Test
    fun testDispatcherProvider_createsConfiguredDispatchers() {
        val testDispatcher = StandardTestDispatcher()
        val dispatchers = TestDispatcherProvider.create(testDispatcher)

        assertSame(testDispatcher, dispatchers.io)
        assertSame(testDispatcher, dispatchers.default)
        assertSame(testDispatcher, dispatchers.main)
    }

    @Test
    fun testDispatcherProvider_unconfinedReturnsUnconfinedDispatchers() = runTest {
        val dispatchers = TestDispatcherProvider.unconfined()
        var runCount = 0

        launch(dispatchers.main) {
            runCount++
        }

        // Unconfined executes immediately without needing advanceUntilIdle
        assertEquals(1, runCount)
    }
}
