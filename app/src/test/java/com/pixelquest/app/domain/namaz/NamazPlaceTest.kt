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
        assertEquals(NamazPlace.LAHORE, NamazPlaceResolver(FakeNamazRepository()).currentPlace())
    }

    @Test
    fun pickingACity_savesIt_andKeepsTheOtherSettings() = runBlocking {
        val repository = FakeNamazRepository(com.pixelquest.app.data.local.entity.NamazSettingsEntity(method = "EGYPTIAN"))
        val resolver = NamazPlaceResolver(repository)

        resolver.chooseCity(NamazCities.find("Islamabad")!!)

        val saved = repository.getSettings()
        assertEquals("Islamabad", saved.city)
        assertEquals(33.6844, saved.latitude!!, 0.0001)
        assertEquals("EGYPTIAN", saved.method)
        assertEquals("Islamabad", resolver.currentPlace().name)
    }
}
