package com.pixelquest.tools.quran

/**
 * The checks every build must pass before it writes anything (docs/quran-data.md, Checks): the
 * standard count, each surah numbered in order and as long as the metadata says, and no empty text.
 */
object Validation {
    const val SURAHS = 114
    const val AYAHS = 6236

    /** Fails, naming the first problem, unless [arabic], [translation] and [metadata] agree as they must. */
    fun check(arabic: ArabicText, translation: TranslationText, metadata: Metadata) {
        fail(arabic.surahs.size != SURAHS) { "the Arabic has ${arabic.surahs.size} surahs, not $SURAHS" }
        fail(arabic.surahs.map { it.number } != (1..SURAHS).toList()) { "the Arabic's surahs aren't numbered 1 to $SURAHS in order" }
        val total = arabic.surahs.sumOf { it.ayahs.size }
        fail(total != AYAHS) { "the Arabic has $total ayahs, not $AYAHS" }

        fail(metadata.surahs.map { it.number } != (1..SURAHS).toList()) { "the metadata's surahs aren't numbered 1 to $SURAHS in order" }
        fail(metadata.surahs.sumOf { it.ayahCount } != AYAHS) { "the metadata counts ${metadata.surahs.sumOf { it.ayahCount }} ayahs, not $AYAHS" }

        arabic.surahs.zip(metadata.surahs).forEach { (surah, info) ->
            fail(surah.ayahs.size != info.ayahCount) { "surah ${surah.number} has ${surah.ayahs.size} ayahs, but the metadata says ${info.ayahCount}" }
            fail(surah.ayahs.map { it.number } != (1..surah.ayahs.size).toList()) { "surah ${surah.number}'s ayahs aren't numbered from 1 in order" }
            surah.ayahs.firstOrNull { it.text.isBlank() }?.let { fail(true) { "${surah.number}:${it.number} has no text" } }
        }

        fail(translation.ayahs.size != AYAHS) { "${translation.id} has ${translation.ayahs.size} ayahs, not $AYAHS" }
        translation.ayahs.entries.firstOrNull { it.value.isBlank() }?.let { (key, _) ->
            fail(true) { "${translation.id}'s ${key.first}:${key.second} has no text" }
        }
    }

    private inline fun fail(failed: Boolean, message: () -> String) {
        if (failed) throw QuranBuildException(message())
    }
}
