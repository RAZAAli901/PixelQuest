package com.pixelquest.app.quran.ui.search

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Marking the matched words: as the search matches them, and never changing the text. */
class SearchHighlightTest {

    private fun marked(text: String, query: String) = SearchHighlight.ranges(text, query).map { text.substring(it) }

    @Test
    fun theLastWord_matchesTheStartOfWords_inAnyCase() {
        assertEquals(listOf("Merciful", "Merciful"), marked("the Entirely Merciful, the Especially Merciful", "merc"))
    }

    @Test
    fun earlierWords_matchOnlyWhole() {
        assertEquals(listOf("Lord", "the"), marked("Praise to Allah, Lord of the worlds", "the lord"))
        assertTrue("\"the\" isn't the start of \"them\" here", marked("them", "the lord").isEmpty())
    }

    @Test
    fun accents_dontMatter() {
        assertEquals(listOf("Musa"), marked("And We gave Musa the Scripture", "mūsa"))
    }

    @Test
    fun theTextItself_isUntouched() {
        val text = "Allah - there is no deity except Him, the Ever-Living."
        val annotated = SearchHighlight.annotate(text, "living", SpanStyle(fontWeight = FontWeight.Bold))

        assertEquals(text, annotated.text)
        assertEquals(1, annotated.spanStyles.size)
        assertEquals("Living", text.substring(annotated.spanStyles[0].start, annotated.spanStyles[0].end))
    }

    @Test
    fun noWords_markNothing() {
        assertTrue(marked("anything", " ?! ").isEmpty())
    }
}
