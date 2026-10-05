package com.pixelquest.app.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * The time as screens see it. Screens whose data depends on "today" build on these flows instead
 * of reading LocalDate.now() once, so a screen kept alive overnight (a tab in the back stack, the
 * app left open) shows the new day. Tests substitute a fixed clock: the real flows never complete,
 * which would keep a test's virtual time running forever.
 */
open class AppClock @Inject constructor() {

    open fun now(): LocalDateTime = LocalDateTime.now()

    /** Emits now, then at the start of every minute. */
    open fun minuteTicks(): Flow<LocalDateTime> = flow {
        while (true) {
            val now = now()
            emit(now)
            delay(Duration.between(now, now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)).toMillis())
        }
    }

    /** Today's date, emitted when collection starts and again just after each midnight. */
    open fun today(): Flow<LocalDate> = flow {
        while (true) {
            val now = now()
            emit(now.toLocalDate())
            delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1)
        }
    }
}
