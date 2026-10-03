package com.pixelquest.app.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Today's date, emitted when collection starts and again just after each midnight. Screens whose
 * data depends on "today" build on this instead of reading LocalDate.now() once, so a screen kept
 * alive overnight (a tab in the back stack, the app left open) shows the new day.
 */
fun todayFlow(): Flow<LocalDate> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now.toLocalDate())
        delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1)
    }
}
