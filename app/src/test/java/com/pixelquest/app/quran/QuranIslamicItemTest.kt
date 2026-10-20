package com.pixelquest.app.quran

import com.pixelquest.app.R
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Quran's card on the Islamic hub: always there, after Namaz and Darood, with no badge. */
class QuranIslamicItemTest {

    @Test
    fun theCard_isAlwaysShown_andOpensTheQuran() = runTest {
        val card = QuranIslamicItem().item().first()

        assertEquals("quran", card.id)
        assertEquals(R.string.quran_title, card.titleRes)
        assertEquals(Screen.Quran.route, card.route)
        assertTrue(Routes.isIslamic(card.route))
        assertTrue(card.enabled)
        assertEquals(0, card.badgeCount)
    }

    @Test
    fun itComesAfterNamazAndDarood() {
        assertTrue(QuranIslamicItem().order > 20)
    }
}
