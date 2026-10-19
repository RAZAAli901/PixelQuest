package com.pixelquest.app.darood.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Usernames: 3 to 20 of a-z, 0-9 and _, stored in lowercase (docs/darood-design.md, Usernames). */
class UsernameTest {

    @Test
    fun aValidName_isKept_asTyped() {
        assertEquals("aria_k99", Username.parse("aria_k99")?.value)
    }

    @Test
    fun whatsTyped_isTrimmed_lowercased_andLosesALeadingAt() {
        assertEquals("aria_k", Username.normalise("  @Aria_K "))
        assertEquals("aria_k", Username.parse("@ARIA_K")?.value)
    }

    @Test
    fun lowercasing_ignoresThePhonesLanguage() {
        val turkish = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr"))
            // In Turkish, "I".lowercase() is a dotless ı, which isn't allowed; the root locale gives i.
            assertEquals("ibrahim", Username.parse("IBRAHIM")?.value)
        } finally {
            java.util.Locale.setDefault(turkish)
        }
    }

    @Test
    fun theLengthLimits_countAfterNormalising() {
        assertEquals(UsernameProblem.EMPTY, Username.problemWith("   "))
        assertEquals(UsernameProblem.EMPTY, Username.problemWith("@"))
        assertEquals(UsernameProblem.TOO_SHORT, Username.problemWith("ab"))
        assertEquals(UsernameProblem.TOO_SHORT, Username.problemWith(" @ab "))
        assertNull(Username.problemWith("abc"))
        assertNull(Username.problemWith("a".repeat(20)))
        assertEquals(UsernameProblem.TOO_LONG, Username.problemWith("a".repeat(21)))
    }

    @Test
    fun onlyLettersDigitsAndUnderscores_areAllowed() {
        listOf("aria k", "aria-k", "aria.k", "ária", "ariа" /* Cyrillic а */, "aria!", "a@ria", "😀aria").forEach {
            assertEquals(it, UsernameProblem.INVALID_CHARACTERS, Username.problemWith(it))
            assertNull(it, Username.parse(it))
        }
        listOf("___", "123", "_a_1").forEach { assertNull(it, Username.problemWith(it)) }
    }
}
