package com.pixelquest.app.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Test rule and validator ensuring that tests and components under test properly inject
 * and query [FakeClock], failing if the injected clock was bypassed (indicating real system time was read).
 */
class ClockInjectionCheck(
    private val clock: FakeClock,
    private val requireAccess: Boolean = true,
) : TestWatcher() {

    public override fun starting(description: Description) {
        clock.resetAccessCount()
    }

    public override fun finished(description: Description) {
        if (requireAccess && !clock.wasAccessed) {
            throw AssertionError(
                "Clock injection check failed: ${description.methodName} did not query the injected FakeClock. " +
                    "Tests must inject the Clock and must not read real system time.",
            )
        }
    }

    companion object {
        /**
         * Fails if the provided [clock] was never accessed during execution.
         */
        fun assertClockQueried(
            clock: FakeClock,
            message: String = "Injected FakeClock was never queried! Real system time was likely used.",
        ) {
            if (!clock.wasAccessed) {
                throw AssertionError(message)
            }
        }
    }
}
