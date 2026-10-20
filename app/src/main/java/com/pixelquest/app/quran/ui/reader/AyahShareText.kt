package com.pixelquest.app.quran.ui.reader

import com.pixelquest.app.quran.domain.Ayah

/**
 * What goes with an ayah when it's copied or shared, already in the player's language.
 *
 * @param reference where it is: "— Al-Baqara 2:255".
 * @param translation whose translation it is: "Translation: Saheeh International".
 * @param text where the Arabic is from, as its terms ask: "Quran text: Tanzil.net".
 */
data class ShareCredits(val reference: String, val translation: String, val text: String)

/**
 * The text an ayah is copied or shared as: the ayah first, exactly as its source gives it, never
 * shortened or changed; then a blank line, its reference, and the credit for what was copied.
 */
object AyahShareText {
    fun arabic(ayah: Ayah, credits: ShareCredits): String =
        listOf(ayah.arabic, "", credits.reference, credits.text).joinToString("\n")

    fun translation(ayah: Ayah, credits: ShareCredits): String =
        listOf(ayah.translation, "", credits.reference, credits.translation).joinToString("\n")

    /** Shared, it's both: the Arabic, its translation, then the reference and both credits. */
    fun both(ayah: Ayah, credits: ShareCredits): String =
        listOf(ayah.arabic, "", ayah.translation, "", credits.reference, credits.translation, credits.text).joinToString("\n")
}
