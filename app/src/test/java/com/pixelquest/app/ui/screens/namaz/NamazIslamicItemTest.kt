package com.pixelquest.app.ui.screens.namaz

import com.pixelquest.app.R
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import com.pixelquest.app.ui.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The Namaz Tracker's card on the Islamic hub: there while available, with the prayers to mark. */
class NamazIslamicItemTest {

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
    private val available = MutableStateFlow(true)
    private val settings = object : FakeSettingsRepository() {
        override val namazAvailable: Flow<Boolean> = available
    }

    /** At [time] in Lahore on [day]: at 16:00, Fajr, Dhuhr and Asr have started, and Maghrib is next. */
    private fun item(time: LocalTime = LocalTime.of(16, 0)): NamazIslamicItem {
        val clock = FixedClock(LocalDateTime.of(day, time))
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
        return NamazIslamicItem(settings, repository, NextPrayerUseCase(provider), clock) { LahoreZone }
    }

    @Test
    fun theCard_opensTheTracker_withItsTitle() = runTest {
        val card = item().item().first()

        assertEquals("namaz", card.id)
        assertEquals(R.string.namaz_title, card.titleRes)
        assertEquals(Screen.Namaz.route, card.route)
        assertEquals(R.plurals.islamic_namaz_badge, card.badgeLabel)
        assertTrue(card.enabled)
    }

    @Test
    fun theBadge_countsThePrayersThatHaveStarted_andArentPrayed() = runTest {
        assertEquals("Fajr, Dhuhr and Asr", 3, item().item().first().badgeCount)

        repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = 0)
        repository.logPrayer(day, Prayer.DHUHR, prayedAtMillis = 0)

        assertEquals("Asr is left", 1, item().item().first().badgeCount)
    }

    @Test
    fun earlyInTheDay_onlyFajrCounts() = runTest {
        assertEquals(1, item(LocalTime.of(7, 0)).item().first().badgeCount)
    }

    @Test
    fun withTheTrackerOff_noBadge_butTheCardStays() = runTest {
        repository.saveSettings(NamazSettingsEntity(enabled = false))

        val card = item().item().first()
        assertEquals(0, card.badgeCount)
        assertTrue("its screen is where it's switched on", card.enabled)
    }

    @Test
    fun whenNamazIsUnavailable_theCardIsHidden() = runTest {
        available.value = false

        assertFalse(item().item().first().enabled)
    }
}
