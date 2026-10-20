package com.pixelquest.tools.quran

/**
 * The Bismillah rule (docs/quran-data.md):
 * - It's stored apart from the ayahs, and shown above every surah except At-Tawbah (9), which has none.
 * - It counts as an ayah only in Al-Fatihah (1), where it's the first ayah itself: so it isn't
 *   shown again above it.
 *
 * Tanzil's XML already keeps it that way: a `bismillah` attribute on the first ayah of every other
 * surah. [check] makes sure it does. Each surah keeps its own, exactly as Tanzil gives it: they
 * aren't all alike (95's and 97's carry a shadda the others don't, as their Mushaf does).
 */
object Bismillah {
    const val AL_FATIHAH = 1
    const val AT_TAWBAH = 9

    /** Whether a separate Bismillah is shown above [surah]: all but Al-Fatihah and At-Tawbah. */
    fun shownAbove(surah: Int): Boolean = surah != AL_FATIHAH && surah != AT_TAWBAH

    /** Fails unless [arabic]'s Bismillahs follow the rule. */
    fun check(arabic: ArabicText) {
        arabic.surahs.forEach { surah ->
            when {
                !shownAbove(surah.number) && surah.bismillah != null ->
                    throw QuranBuildException("surah ${surah.number} mustn't have a separate Bismillah")
                shownAbove(surah.number) && surah.bismillah.isNullOrEmpty() ->
                    throw QuranBuildException("surah ${surah.number} has no Bismillah")
            }
        }
    }
}
