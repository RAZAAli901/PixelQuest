package com.pixelquest.app.quran.ui.search

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import java.text.Normalizer

/**
 * Marking, in a translation, the words a search matched: only styling over the text, which stays
 * exactly as it is. It matches as the full-text search does (TranslationSearch): case and accents
 * don't matter, each word is whole but the last, which may be the start of a word.
 */
object SearchHighlight {
    private val WORD = Regex("[\\p{L}\\p{N}]+")

    /** Where [text] has [query]'s words, as ranges of its characters. */
    fun ranges(text: String, query: String): List<IntRange> {
        val words = WORD.findAll(query).map { fold(it.value) }.toList()
        if (words.isEmpty()) return emptyList()
        val whole = words.dropLast(1).toSet()
        val start = words.last()
        return WORD.findAll(text)
            .filter { match -> fold(match.value).let { it in whole || it.startsWith(start) } }
            .map { it.range }
            .toList()
    }

    /** [text] with [style] over the words [query] matched. */
    fun annotate(text: String, query: String, style: SpanStyle): AnnotatedString = buildAnnotatedString {
        append(text)
        ranges(text, query).forEach { addStyle(style, it.first, it.last + 1) }
    }

    /** Lowercase, without accents. */
    private fun fold(word: String): String =
        Normalizer.normalize(word, Normalizer.Form.NFD).filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }.lowercase()
}
