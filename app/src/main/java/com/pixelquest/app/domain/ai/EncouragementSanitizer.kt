package com.pixelquest.app.domain.ai

/**
 * Cleans AI-written reminder lines before they reach a notification. Anything that looks off
 * (links, markup, game words in Simple Mode, too long) is dropped rather than repaired, and the
 * caller falls back to [StaticEncouragementBank].
 */
object EncouragementSanitizer {

    const val MAX_CHARS = 90

    private val gameWords = listOf("quest", "hero", "xp", "level", "streak", "loot", "boss", "dungeon", "realm")
    private val bannedFragments = listOf("http", "www.", "@", "#", "<", ">", "{", "}", "[", "]")

    fun clean(lines: List<String>, isSimpleMode: Boolean): List<String> =
        lines.mapNotNull { cleanLine(it, isSimpleMode) }.distinct()

    internal fun cleanLine(raw: String, isSimpleMode: Boolean): String? {
        val line = raw
            .replace(Regex("[*_`~]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('"', '\'', '“', '”', '‘', '’')
            .trim()
        if (line.length < 8 || line.length > MAX_CHARS) return null
        val lower = line.lowercase()
        if (bannedFragments.any { lower.contains(it) }) return null
        if (isSimpleMode && gameWords.any { Regex("\\b$it").containsMatchIn(lower) }) return null
        return line
    }
}
