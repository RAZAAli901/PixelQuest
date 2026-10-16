package com.pixelquest.app.domain.namaz

import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.namaz.LocationProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the tracker's times are for (docs/namaz-design.md, Location): the saved place, a city the
 * player picked, or Lahore when there's neither.
 */
@Singleton
class NamazPlaceResolver @Inject constructor(
    private val repository: NamazRepository,
    private val locationProvider: LocationProvider
) {

    /**
     * Use my location: the phone's rough position, cached in the tracker's settings so the times
     * keep working offline, and named after the nearest listed city within 50km. Null without
     * permission or a fix, and the saved place (or Lahore) stays.
     */
    suspend fun useMyLocation(): NamazPlace? {
        val here = locationProvider.currentLocation() ?: return null
        val nearby = NamazCities.all.minByOrNull { distanceKm(here.latitude, here.longitude, it.latitude, it.longitude) }
            ?.takeIf { distanceKm(here.latitude, here.longitude, it.latitude, it.longitude) <= NEARBY_CITY_KM }
        val place = NamazPlace(nearby?.name ?: "", here.latitude, here.longitude)
        val settings = repository.getSettings()
        repository.saveSettings(settings.copy(latitude = place.latitude, longitude = place.longitude, city = place.name.ifBlank { null }))
        return place
    }

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

    companion object {
        /** Within this, a position is named after the city (the list's are city centres). */
        const val NEARBY_CITY_KM = 50.0

        /** Great-circle distance, close enough for naming a city. */
        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
            return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        }
    }
}
