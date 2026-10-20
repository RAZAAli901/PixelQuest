package com.pixelquest.app.quran.ui.reader

import com.pixelquest.app.quran.domain.Ayah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An ayah copied or shared: the ayah first and unchanged, then its reference and credits. The texts
 * here are made up, not the Quran's.
 */
class AyahShareTextTest {

    private val ayah = Ayah(262, 2, 255, "ـنص", "made-up translation", "en.sahih")
    private val credits = ShareCredits("— Al-Baqara 2:255", "Translation: Saheeh International", "Quran text: Tanzil.net")

    @Test
    fun theArabic_thenItsReference_andTanzilsCredit() {
        assertEquals("ـنص\n\n— Al-Baqara 2:255\nQuran text: Tanzil.net", AyahShareText.arabic(ayah, credits))
    }

    @Test
    fun theTranslation_thenItsReference_andItsTranslator() {
        assertEquals("made-up translation\n\n— Al-Baqara 2:255\nTranslation: Saheeh International", AyahShareText.translation(ayah, credits))
    }

    @Test
    fun shared_itsBoth_withBothCredits() {
        val shared = AyahShareText.both(ayah, credits)

        assertTrue("the ayah first, unchanged", shared.startsWith(ayah.arabic + "\n\n" + ayah.translation + "\n\n"))
        assertTrue(shared.endsWith("— Al-Baqara 2:255\nTranslation: Saheeh International\nQuran text: Tanzil.net"))
    }
}
