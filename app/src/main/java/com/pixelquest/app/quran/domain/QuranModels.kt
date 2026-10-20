package com.pixelquest.app.quran.domain

/** Where a surah was revealed, as Tanzil's metadata says. */
enum class Revelation {
    MECCAN,
    MEDINAN;

    companion object {
        /** Tanzil writes `Meccan` or `Medinan`. */
        fun of(source: String): Revelation = if (source.equals("Medinan", ignoreCase = true)) MEDINAN else MECCAN
    }
}

/**
 * A surah's details (docs/quran-data.md).
 *
 * @param englishName its name in English letters (Al-Faatiha); [englishMeaning] what it means (The Opening).
 * @param juz the juz its first ayah is in.
 * @param bismillah the Bismillah shown above it, as the source gives it; null for Al-Fatihah (whose
 *   first ayah it is) and At-Tawbah (which has none).
 */
data class Surah(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishMeaning: String,
    val revelation: Revelation,
    val ayahCount: Int,
    val juz: Int,
    val bismillah: String?
)

/**
 * One ayah, its Arabic and its translation, exactly as their sources give them.
 *
 * @param id its place in the whole Quran, 1 to 6,236.
 * @param translationId the translation [translation] is from.
 */
data class Ayah(
    val id: Int,
    val surah: Int,
    val number: Int,
    val arabic: String,
    val translation: String,
    val translationId: String
)

/** Where a juz starts: its surah (with that surah's English name) and ayah. */
data class Juz(val number: Int, val surah: Int, val ayah: Int, val surahName: String)

/** A translation on the phone: its credits and terms, for the About screen. */
data class Translation(
    val id: String,
    val name: String,
    val translator: String,
    val language: String,
    val lastUpdate: String,
    val licenceNote: String,
    val notice: String
)
