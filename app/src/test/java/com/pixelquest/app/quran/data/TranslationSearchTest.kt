package com.pixelquest.app.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the player types becomes a safe full-text query: words only, the last one still being typed,
 * its prefix star inside its quotes (after them, FTS matches only the whole word).
 */
class TranslationSearchTest {

    @Test
    fun oneWord_matchesAsItsStartToo() {
        assertEquals("\"merc*\"", TranslationSearch.match("merc"))
    }

    @Test
    fun severalWords_allMustBeThere() {
        assertEquals("the player's own capitals: the index ignores case", "\"Lord\" \"of\" \"the\" \"worlds*\"", TranslationSearch.match("Lord of the worlds"))
    }

    @Test
    fun querySyntax_isNeverRead() {
        // Quotes, stars, OR, NEAR, minus signs and brackets are just text to the player.
        assertEquals("\"light\" \"OR\" \"dark*\"", TranslationSearch.match("light OR \"dark*"))
        assertEquals("\"a\" \"b*\"", TranslationSearch.match("-a (b)"))
    }

    @Test
    fun nothingButSpacesOrMarks_isNoSearch() {
        assertNull(TranslationSearch.match(""))
        assertNull(TranslationSearch.match("  ?! \"\" "))
    }

    @Test
    fun lettersAndDigitsOfAnyScript_count() {
        assertEquals("\"ayah\" \"255*\"", TranslationSearch.match("ayah 255"))
    }
}
