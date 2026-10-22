package com.pixelquest.app.domain.ai

/**
 * The model's words as plain text (S051). Compose shows text as it is, so nothing here could run;
 * this makes sure it also reads as plain text: no markdown, HTML tags or link syntax, no control
 * characters, one paragraph, and no longer than a card has room for.
 */
object AiText {

    const val MAX_CHARS = 600

    private val MARKDOWN_LINK = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val TAG = Regex("""<[^>]{0,200}>""")
    // A single underscore stays: category names like SELF_CARE use it.
    private val EMPHASIS = Regex("""\*+|`+|~~|__+""")
    private val HEADING = Regex("""(^|\s)#{1,6}\s""")
    private val SPACE = Regex("""\s+""")

    fun plain(raw: String): String = raw
        .filterNot { Character.isISOControl(it) && it != '\n' && it != '\t' }
        .replace(MARKDOWN_LINK, "$1")
        .replace(TAG, "")
        .replace(EMPHASIS, "")
        .replace(HEADING, "$1")
        .replace(SPACE, " ")
        .trim()
        .take(MAX_CHARS)
}
