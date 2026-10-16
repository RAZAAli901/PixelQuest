package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the tracker's times are for (docs/namaz-design.md, Location): the saved place, a city the
 * player picked, or Lahore when there's neither.
 */
@Singleton
class NamazPlaceResolver @Inject constructor(
    private val repository: NamazRepository
) {

    /** The place the times use now: the saved one, or Lahore. */
    suspend fun currentPlace(): NamazPlace {
        val settings = repository.getSettings()
        return DefaultPrayerTimeProvider.placeOf(settings.latitude, settings.longitude, settings.city)
    }

    /** The player picked [place] (from [NamazCities], or after refusing location): the times follow it. */
    suspend fun chooseCity(place: NamazPlace) {
        val settings = repository.getSettings()
        repository.saveSettings(settings.copy(latitude = place.latitude, longitude = place.longitude, city = place.name))
    }
}
