package com.pixelquest.app.domain

/**
 * Domain utility providing client-side moderation for public leaderboard display names.
 * Ensures community safety by filtering profanity, hate speech, and offensive terms
 * including common l33tspeak and character substitution variants.
 */
object DisplayNameModerator {

    // Curated blacklist of offensive terms, profanity, and hate speech
    private val BLOCKED_TERMS = listOf(
        "asshole", "bastard", "bitch", "blowjob", "crap",
        "cunt", "damn", "dick", "dildo", "douche",
        "fag", "faggot", "fuck", "homo", "nazi",
        "nigga", "nigger", "penis", "piss", "prick",
        "pussy", "retard", "shit", "slut", "tit",
        "twat", "vagina", "whore"
    )

    /**
     * Normalizes an input string to detect evasion techniques:
     * - Converts to lowercase
     * - Maps common l33tspeak characters to standard alphabet ('@'/'4' -> 'a', '1'/'!' -> 'i', etc.)
     * - Removes special characters and repeating redundant characters
     */
    fun normalize(input: String): String {
        var normalized = input.lowercase()
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

    /**
     * Returns true if the provided display name is safe and does not contain
     * any blocked offensive terminology.
     */
    fun isAppropriate(name: String): Boolean {
        val trimmed = name.trim().lowercase()
        if (trimmed.isEmpty()) return true

        // Direct check
        for (term in BLOCKED_TERMS) {
            if (trimmed.contains(term)) {
                return false
            }
        }

        // Normalized check (with leetspeak substitutions collapsed)
        val normalized = normalize(name).replace(" ", "")
        for (term in BLOCKED_TERMS) {
            if (normalized.contains(term)) {
                return false
            }
        }

        return true
    }

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
