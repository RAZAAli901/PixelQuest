package com.pixelquest.app.testing

import com.pixelquest.app.di.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Test factory and provider for supplying controlled [CoroutineDispatchers] in unit and integration tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TestDispatcherProvider {

    /**
     * Creates a [CoroutineDispatchers] instance where [io], [default], and [main] all use
     * the specified [testDispatcher] (defaulting to [StandardTestDispatcher]).
     */
    fun create(
        testDispatcher: TestDispatcher = StandardTestDispatcher(),
    ): CoroutineDispatchers = CoroutineDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    /**
     * Creates a [CoroutineDispatchers] instance using [UnconfinedTestDispatcher] for eager execution.
     */
    fun unconfined(
        testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
    ): CoroutineDispatchers = CoroutineDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    /**
     * Creates a custom [CoroutineDispatchers] with specific test dispatchers for each lane.
     */
    fun custom(
        io: CoroutineDispatcher = StandardTestDispatcher(),
        default: CoroutineDispatcher = StandardTestDispatcher(),
        main: CoroutineDispatcher = StandardTestDispatcher(),
    ): CoroutineDispatchers = CoroutineDispatchers(
        io = io,
        default = default,
        main = main,
    )
}
