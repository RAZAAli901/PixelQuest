package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.util.AppClock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Where the times are worked out for. */
data class NamazPlace(val name: String, val latitude: Double, val longitude: Double) {
    companion object {
        /** Until the player gives a place (docs/namaz-design.md). */
        val LAHORE = NamazPlace("Lahore", 31.5204, 74.3587)
    }
}

/** A prayer day's times, with the tracker's settings and place, in the phone's time zone. */
interface PrayerTimeProvider {

    suspend fun timesFor(date: LocalDate): PrayerDayTimes

    /** Today's and tomorrow's, by the phone's clock (tomorrow's Fajr is the next prayer after Isha). */
    suspend fun todayAndTomorrow(): Pair<PrayerDayTimes, PrayerDayTimes> {
        val today = today()
        return timesFor(today) to timesFor(today.plusDays(1))
    }

    fun today(): LocalDate
}

@Singleton
class DefaultPrayerTimeProvider(
    private val repository: NamazRepository,
    private val calculator: PrayerTimesCalculator,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) : PrayerTimeProvider {

    @Inject
    constructor(repository: NamazRepository, calculator: PrayerTimesCalculator, clock: AppClock) :
        this(repository, calculator, clock, { ZoneId.systemDefault() })

    override fun today(): LocalDate = clock.now().toLocalDate()

    override suspend fun timesFor(date: LocalDate): PrayerDayTimes {
        val settings = repository.getSettings()
        val place = placeOf(settings.latitude, settings.longitude, settings.city)
        return calculator.calculate(date, place.latitude, place.longitude, zone(), settings.toCalcSettings())
    }

    companion object {
        /** The saved place, or Lahore when there isn't one (or it's out of range). */
        fun placeOf(latitude: Double?, longitude: Double?, city: String?): NamazPlace =
            if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                NamazPlace(city ?: "", latitude, longitude)
            } else {
                NamazPlace.LAHORE
            }
    }
}
