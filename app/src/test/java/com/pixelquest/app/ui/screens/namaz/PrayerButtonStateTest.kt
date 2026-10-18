package com.pixelquest.app.ui.screens.namaz

import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.testing.LahoreZone
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/** Which of the four looks a prayer's button gets. */
class PrayerButtonStateTest {

    private val time = LocalDateTime.of(2026, 10, 17, 15, 50).atZone(LahoreZone)

    private fun slot(status: PrayerStatus, started: Boolean) =
        PrayerSlot(Prayer.ASR, time, time.plusHours(2), status, started)

    @Test
    fun eachSlot_getsItsState() {
        assertEquals(PrayerButtonState.LOCKED, PrayerButtonState.of(slot(PrayerStatus.PENDING, started = false)))
        assertEquals(PrayerButtonState.PENDING, PrayerButtonState.of(slot(PrayerStatus.PENDING, started = true)))
        assertEquals(PrayerButtonState.PRAYED, PrayerButtonState.of(slot(PrayerStatus.PRAYED, started = true)))
        assertEquals(PrayerButtonState.MISSED, PrayerButtonState.of(slot(PrayerStatus.MISSED, started = true)))
    }

    @Test
    fun everyPrayer_hasItsOwnPicture() {
        assertEquals(Prayer.entries.size, Prayer.entries.map { it.icon }.toSet().size)
    }
}
