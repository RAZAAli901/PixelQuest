package com.pixelquest.app.domain.namaz

import java.text.Normalizer

/**
 * Cities to pick from when location isn't allowed or there's no fix (docs/namaz-design.md):
 * Pakistan's largest first, then the Gulf and Hijaz, then others with large Muslim communities.
 * City-level coordinates are all prayer times need.
 */
object NamazCities {

    val all: List<NamazPlace> = listOf(
        NamazPlace.LAHORE,
        NamazPlace("Karachi", 24.8607, 67.0011),
        NamazPlace("Islamabad", 33.6844, 73.0479),
        NamazPlace("Rawalpindi", 33.5651, 73.0169),
        NamazPlace("Faisalabad", 31.4504, 73.1350),
        NamazPlace("Multan", 30.1575, 71.5249),
        NamazPlace("Peshawar", 34.0151, 71.5249),
        NamazPlace("Quetta", 30.1798, 66.9750),
        NamazPlace("Makkah", 21.3891, 39.8579),
        NamazPlace("Madinah", 24.5247, 39.5692),
        NamazPlace("Riyadh", 24.7136, 46.6753),
        NamazPlace("Dubai", 25.2048, 55.2708),
        NamazPlace("Doha", 25.2854, 51.5310),
        NamazPlace("Istanbul", 41.0082, 28.9784),
        NamazPlace("Cairo", 30.0444, 31.2357),
        NamazPlace("Dhaka", 23.8103, 90.4125),
        NamazPlace("Delhi", 28.7041, 77.1025),
        NamazPlace("Jakarta", -6.2088, 106.8456),
        NamazPlace("Kuala Lumpur", 3.1390, 101.6869),
        NamazPlace("London", 51.5074, -0.1278),
        NamazPlace("New York", 40.7128, -74.0060),
        NamazPlace("Toronto", 43.6532, -79.3832)
    )

    /** Cities whose name contains [query], ignoring case and accents; all of them for a blank query. */
    fun search(query: String): List<NamazPlace> {
        val wanted = fold(query)
        if (wanted.isBlank()) return all
        return all.filter { fold(it.name).contains(wanted) }
    }

    /** The city with exactly [name] (ignoring case and accents), if it's in the list. */
    fun find(name: String?): NamazPlace? = name?.let { n -> all.firstOrNull { fold(it.name) == fold(n) } }

    private fun fold(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
}
