package com.pixelquest.app.darood.domain

import java.util.Locale

/** Why something typed can't be a username. */
enum class UsernameProblem { EMPTY, TOO_SHORT, TOO_LONG, INVALID_CHARACTERS, RESERVED }

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
                isReserved(name) -> UsernameProblem.RESERVED
                else -> null
            }
        }

        /**
         * Names that could pass for the app or its staff. Underscores and trailing digits don't
         * get around it: `admin`, `Admin_1` and `pixel_quest` are all reserved. The server holds
         * the same list (supabase/migrations, usernames).
         */
        val RESERVED: Set<String> = setOf(
            "admin", "administrator", "support", "help", "pixelquest", "moderator", "mod",
            "staff", "official", "system", "root", "null", "undefined"
        )

        /** Whether [name] (already normalised) is, or dresses up, a reserved word. */
        fun isReserved(name: String): Boolean = name.replace("_", "").trimEnd { it.isDigit() } in RESERVED

        /** The username [input] makes, or null if it breaks a rule ([problemWith] says which). */
        fun parse(input: String): Username? = if (problemWith(input) == null) Username(normalise(input)) else null
    }
}
