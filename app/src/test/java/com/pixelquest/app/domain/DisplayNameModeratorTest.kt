package com.pixelquest.app.domain

import com.pixelquest.app.ui.screens.account.AccountViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayNameModeratorTest {

    @Test
    fun validCleanNames_areAllowed() {
        val safeNames = listOf(
            "PixelKnight",
            "Hero_99",
            "RetroGamer",
            "Super_Mario",
            "Nova_Champion",
            "DragonSlayer"
        )

        for (name in safeNames) {
            assertTrue("Expected safe name '$name' to be appropriate", DisplayNameModerator.isAppropriate(name))
            assertNull("Expected null error for safe name '$name'", DisplayNameModerator.validate(name))
        }
    }

    @Test
    fun directProfanity_isBlocked() {
        val offensiveNames = listOf(
            "asshole_king",
            "bitch_mode",
            "shit_talker",
            "fag_destroyer",
            "pussy_cat",
            "dick_head"
        )

        for (name in offensiveNames) {
            assertFalse("Expected profanity '$name' to be blocked", DisplayNameModerator.isAppropriate(name))
            assertNotNull("Expected error message for '$name'", DisplayNameModerator.validate(name))
            assertEquals(
                "Display name contains disallowed or offensive language.",
                DisplayNameModerator.validate(name)
            )
        }
    }

    @Test
    fun caseInsensitiveProfanity_isBlocked() {
        val mixedCase = listOf(
            "BiTcH",
            "SHIT_HEAD",
            "fUcK_yOu",
            "AsShOlE"
        )

        for (name in mixedCase) {
            assertFalse("Expected '$name' to be blocked regardless of case", DisplayNameModerator.isAppropriate(name))
        }
    }

    @Test
    fun leetspeakSubstitutions_areDetectedAndBlocked() {
        val leetNames = listOf(
            "b1tch",
            "f@ck_you",
            "a$$hole",
            "sh!t",
            "d1ck"
        )

        for (name in leetNames) {
            assertFalse("Expected leetspeak '$name' to be blocked", DisplayNameModerator.isAppropriate(name))
            assertNotNull("Expected error for '$name'", DisplayNameModerator.validate(name))
        }
    }

    @Test
    fun characterRepeatingEvasion_isBlocked() {
        val repeated = listOf(
            "fuuuuck",
            "shiiiiit",
            "biitcch"
        )

        for (name in repeated) {
            assertFalse("Expected repeated letter evasion '$name' to be blocked", DisplayNameModerator.isAppropriate(name))
        }
    }

    @Test
    fun accountViewModel_validateDisplayName_integratesModerator() {
        // Blank
        assertEquals("Display name cannot be blank.", AccountViewModel.validateDisplayName(""))
        assertEquals("Display name cannot be blank.", AccountViewModel.validateDisplayName("   "))

        // Length
        assertEquals("Name must be at least 3 characters.", AccountViewModel.validateDisplayName("ab"))
        assertEquals("Name must not exceed 20 characters.", AccountViewModel.validateDisplayName("a".repeat(21)))

        // Regex characters
        assertEquals("Only alphanumeric characters and underscores allowed.", AccountViewModel.validateDisplayName("Hero@123"))
        assertEquals("Only alphanumeric characters and underscores allowed.", AccountViewModel.validateDisplayName("Hero-123"))

        // Moderation filter
        assertEquals(
            "Display name contains disallowed or offensive language.",
            AccountViewModel.validateDisplayName("asshole_99")
        )
        assertEquals(
            "Display name contains disallowed or offensive language.",
            AccountViewModel.validateDisplayName("bad_b1tch")
        )

        // Valid
        assertNull(AccountViewModel.validateDisplayName("Valid_Hero_88"))
    }
}
