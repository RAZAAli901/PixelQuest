package com.pixelquest.app.domain

/**
 * Domain utility providing client-side moderation for public leaderboard display names.
 * Ensures community safety by filtering profanity, hate speech, and offensive terms
 * including common l33tspeak and character substitution variants.
 *
 * Same rules as the server's trigger (supabase/migrations/20261010000000_display_name_word_matching.sql):
 * unambiguous terms are refused anywhere in a name, but words that are only offensive on their own
 * are refused only as whole words, so ordinary names like "Titan_Slayer", "Stitch" or "Scrappy" pass.
 * Both are tested on the names in supabase/tests/display_names.json.
 */
object DisplayNameModerator {

    /** Offensive wherever they appear. */
    private val ANYWHERE_TERMS = listOf(
        "asshole", "bastard", "bitch", "blowjob", "cunt",
        "dildo", "douche", "faggot", "fuck", "nazi",
        "nigga", "nigger", "pussy", "retard", "shit",
        "slut", "vagina", "whore"
    )

    /** Offensive as a word of their own (or its plural), but common inside ordinary names. */
    private val WORD_TERMS = listOf(
        "crap", "damn", "dick", "fag", "homo",
        "penis", "piss", "prick", "tit", "twat"
    )

    /**
     * Normalizes an input string to detect evasion techniques:
     * - Converts to lowercase
     * - Maps common l33tspeak characters to standard alphabet ('@'/'4' -> 'a', '1'/'!' -> 'i', etc.)
     * - Removes special characters and repeating redundant characters
     */
    fun normalize(input: String): String {
        val normalized = substituteLeetspeak(input)

        // Collapse duplicate letters (e.g. "fuuuck" -> "fuck")
        val collapsed = StringBuilder()
        var lastChar: Char? = null
        for (ch in normalized) {
            if (ch != lastChar) {
                collapsed.append(ch)
                lastChar = ch
            }
        }
        return collapsed.toString()
    }

    /** Lowercase with l33tspeak characters mapped back to letters, repeats kept. */
    private fun substituteLeetspeak(input: String): String {
        return input.lowercase()
            .replace('@', 'a')
            .replace('4', 'a')
            .replace('8', 'b')
            .replace('3', 'e')
            .replace('1', 'i')
            .replace('!', 'i')
            .replace('|', 'i')
            .replace('0', 'o')
            .replace('$', 's')
            .replace('5', 's')
            .replace('7', 't')
            .replace('+', 't')
            .replace('_', ' ')
            .replace('-', ' ')
    }

    /**
     * Returns true if the provided display name is safe and does not contain
     * any blocked offensive terminology.
     */
    fun isAppropriate(name: String): Boolean {
        val trimmed = name.trim().lowercase()
        if (trimmed.isEmpty()) return true

        // Anywhere in the name: as typed, with l33tspeak read as letters, and with repeated letters
        // merged too. Merging catches "fuuuck" but turns "a$$hole" into "ashole", so the text
        // before merging is checked as well.
        val normalized = normalize(name).replace(" ", "")
        val substituted = substituteLeetspeak(name).replace(" ", "")
        for (term in ANYWHERE_TERMS) {
            if (trimmed.contains(term) || normalized.contains(term) || substituted.contains(term)) {
                return false
            }
        }

        // Whole words only: "Titan" isn't "tit", but "Big_Tits" and "DickHead" are caught.
        for (word in words(name)) {
            val forms = setOf(substituteLeetspeak(word), normalize(word))
            for (term in WORD_TERMS) {
                if (forms.any { it == term || it == term + "s" }) return false
            }
        }

        return true
    }

    /** A name's words: split at underscores, spaces and hyphens, and where lower case turns upper ("DickHead"). */
    private fun words(name: String): List<String> =
        name.trim()
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .split(Regex("[_\\s-]+"))
            .filter { it.isNotEmpty() }

    /**
     * Validates display name moderation. Returns null if appropriate,
     * or a user-friendly error string if offensive.
     */
    fun validate(name: String): String? {
        if (!isAppropriate(name)) {
            return "Display name contains disallowed or offensive language."
        }
        return null
    }
}
