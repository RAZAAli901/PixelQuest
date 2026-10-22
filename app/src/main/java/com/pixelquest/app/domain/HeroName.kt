package com.pixelquest.app.domain

/**
 * The local hero name's rules, shared by onboarding and Settings: up to 20 characters and not blank.
 * (The public leaderboard name is separate and also moderated; see DisplayNameModerator.)
 */
object HeroName {
    const val MAX_LENGTH = 20

    /** Keeps typing within the limit, without control characters (InputRules, S053). */
    fun clamp(input: String): String = com.pixelquest.app.domain.validation.InputRules.typed(input, com.pixelquest.app.domain.validation.InputRules.Field.HERO_NAME)

    /** Why [name] can't be saved, or null if it can. */
    fun error(name: String): String? = when {
        name.isBlank() -> "Name cannot be blank"
        name.length > MAX_LENGTH -> "Name must be $MAX_LENGTH characters or less"
        else -> null
    }
}
