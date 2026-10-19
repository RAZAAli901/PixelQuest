package com.pixelquest.app.darood.domain

import java.util.Locale

/** Why something typed can't be a username. */
enum class UsernameProblem { EMPTY, TOO_SHORT, TOO_LONG, INVALID_CHARACTERS }

/**
 * A player's username for Darood Parties (docs/darood-design.md, Usernames): 3 to 20 characters
 * of `a`–`z`, `0`–`9` and `_`, always lowercase. Made only through [parse], so a [Username] is
 * always a valid one. The server checks the same rules again; this lets the app say "no" first.
 */
@JvmInline
value class Username private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 20

        private val ALLOWED = Regex("[a-z0-9_]+")

        /** [input] as it would be stored: trimmed, without a leading @, in lowercase. */
        fun normalise(input: String): String = input.trim().removePrefix("@").lowercase(Locale.ROOT)

        /** Why [input] can't be a username, or null when it can (after [normalise]). */
        fun problemWith(input: String): UsernameProblem? {
            val name = normalise(input)
            return when {
                name.isEmpty() -> UsernameProblem.EMPTY
                name.length < MIN_LENGTH -> UsernameProblem.TOO_SHORT
                name.length > MAX_LENGTH -> UsernameProblem.TOO_LONG
                !ALLOWED.matches(name) -> UsernameProblem.INVALID_CHARACTERS
                else -> null
            }
        }

        /** The username [input] makes, or null if it breaks a rule ([problemWith] says which). */
        fun parse(input: String): Username? = if (problemWith(input) == null) Username(normalise(input)) else null
    }
}
