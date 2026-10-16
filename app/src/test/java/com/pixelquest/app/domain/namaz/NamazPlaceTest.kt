package com.pixelquest.app.domain.namaz

import com.pixelquest.app.testing.FakeNamazRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Without location: a city from the list, or Lahore. */
class NamazPlaceTest {

    @Test
    fun theCityList_startsWithLahore_andHasNoDuplicates() {
        assertEquals(NamazPlace.LAHORE, NamazCities.all.first())
        assertEquals(NamazCities.all.size, NamazCities.all.map { it.name }.toSet().size)
        assertTrue(NamazCities.all.all { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 })
    }

    @Test
    fun searching_ignoresCaseAndAccents() {
        assertEquals(listOf("Karachi"), NamazCities.search("kara").map { it.name })
        assertEquals(listOf("Kuala Lumpur"), NamazCities.search("LUMPUR").map { it.name })
        assertEquals("Makkah", NamazCities.find("mákkah")?.name)
        assertEquals(NamazCities.all, NamazCities.search("  "))
        assertNull(NamazCities.find("Atlantis"))
    }

    @Test
    fun withNothingSaved_theTimesAreLahores() = runBlocking {
        assertEquals(NamazPlace.LAHORE, NamazPlaceResolver(FakeNamazRepository(), NoLocation).currentPlace())
    }

    @Test
    fun pickingACity_savesIt_andKeepsTheOtherSettings() = runBlocking {
        val repository = FakeNamazRepository(com.pixelquest.app.data.local.entity.NamazSettingsEntity(method = "EGYPTIAN"))
        val resolver = NamazPlaceResolver(repository, NoLocation)

        resolver.chooseCity(NamazCities.find("Islamabad")!!)

        val saved = repository.getSettings()
        assertEquals("Islamabad", saved.city)
        assertEquals(33.6844, saved.latitude!!, 0.0001)
        assertEquals("EGYPTIAN", saved.method)
        assertEquals("Islamabad", resolver.currentPlace().name)
    }

    private object NoLocation : com.pixelquest.app.namaz.LocationProvider {
        override fun hasPermission() = false
        override suspend fun currentLocation(): com.pixelquest.app.namaz.GeoPoint? = null
    }

    private class At(private val point: com.pixelquest.app.namaz.GeoPoint) : com.pixelquest.app.namaz.LocationProvider {
        override fun hasPermission() = true
        override suspend fun currentLocation() = point
    }

    @Test
    fun useMyLocation_cachesThePosition_namedAfterTheNearbyCity() = runBlocking {
        val repository = FakeNamazRepository()
        // A few km from Lahore's centre.
        val place = NamazPlaceResolver(repository, At(com.pixelquest.app.namaz.GeoPoint(31.47, 74.40))).useMyLocation()!!

        assertEquals("Lahore", place.name)
        val saved = repository.getSettings()
        assertEquals(31.47, saved.latitude!!, 0.0001)
        assertEquals(74.40, saved.longitude!!, 0.0001)
        assertEquals("Lahore", saved.city)
    }

    @Test
    fun useMyLocation_farFromAnyListedCity_keepsThePosition_withoutAName() = runBlocking {
        val repository = FakeNamazRepository()
        // Mid-Pacific: no listed city within 50km.
        NamazPlaceResolver(repository, At(com.pixelquest.app.namaz.GeoPoint(10.0, -150.0))).useMyLocation()

        assertNull(repository.getSettings().city)
        assertEquals(-150.0, repository.getSettings().longitude!!, 0.0001)
    }

    @Test
    fun useMyLocation_withoutPermission_keepsTheSavedPlace() = runBlocking {
        val repository = FakeNamazRepository(com.pixelquest.app.data.local.entity.NamazSettingsEntity(latitude = 24.8607, longitude = 67.0011, city = "Karachi"))

        assertNull(NamazPlaceResolver(repository, NoLocation).useMyLocation())
        assertEquals("Karachi", repository.getSettings().city)
    }

    @Test
    fun distances_areSane() {
        // Lahore to Islamabad is about 270km.
        val km = NamazPlaceResolver.distanceKm(31.5204, 74.3587, 33.6844, 73.0479)
        assertTrue("$km", km in 250.0..290.0)
    }
}
