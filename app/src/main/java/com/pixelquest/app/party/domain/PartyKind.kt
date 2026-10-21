package com.pixelquest.app.party.domain

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * A party's kind, as the server (`parties.party_type`) and the phone's cache name it. Every party is
 * one kind for good (docs/party-core.md).
 */
enum class PartyType(val wire: String) {
    DAROOD("darood"),
    STUDY("study");

    companion object {
        /** The kind [wire] names; an unknown or missing one is a Darood Party, as every party before kinds was. */
        fun fromWire(wire: String?): PartyType = entries.firstOrNull { it.wire == wire } ?: DAROOD
    }
}

/** A tab on a party's own screen. Each kind shows its own ([PartyKind.tabs]). */
enum class PartyTab { CHAT, PEOPLE, THIS_WEEK, ALL_ITEMS, SCOREBOARD, FILES, CHESS }

/**
 * What a kind of party may do beyond the shared core, as its server allows it.
 *
 * @param milestone it has a milestone its members race to, and finishes on reaching it.
 * @param raceBoard its leader can put it on the public race board.
 * @param weeklyNote a weekly note of the days the player took part, which they can turn on.
 */
data class PartyKindRules(val milestone: Boolean, val raceBoard: Boolean, val weeklyNote: Boolean)

/** A piece of text in a [PartySummary], from a string or plural resource. */
sealed interface Phrase {
    data class Text(@StringRes val res: Int, val args: List<Any> = emptyList()) : Phrase
    data class Plural(@PluralsRes val res: Int, val count: Int, val args: List<Any> = listOf(count)) : Phrase
}

/**
 * What a party's row in a list says under its name: [main], its phrases side by side; then a bar
 * at [progress] (0 to 1), if it has one; then [detail].
 */
data class PartySummary(val main: List<Phrase>, val detail: Phrase? = null, val progress: Float? = null)

/**
 * A kind of party: the shared core (members, invitations, the leader, the 2-30 limit) is the same
 * for every kind; this is what's its own. Its name and icon, what its rows in a list summarise, the
 * tabs its own screen shows, and its own rules.
 */
interface PartyKind {
    val type: PartyType

    /** One of its parties: "Darood Party". */
    @get:StringRes
    val titleRes: Int

    /** Its parties' screen, listing them: "Darood Party", "Study parties". */
    @get:StringRes
    val listTitleRes: Int

    /** What its list says before the player has any: what its parties are, and how to start one. */
    @get:StringRes
    val emptyBodyRes: Int

    /** What its list says signed out: why its parties need an account. */
    @get:StringRes
    val signedOutBodyRes: Int

    /** What handing the leadership over means, in its parties: what the new leader will do. */
    @get:StringRes
    val handOverRes: Int

    /** An emoji for it, beside its name where both kinds appear together. */
    val icon: String

    /** The tabs its party screen shows, in order. */
    val tabs: List<PartyTab>

    val rules: PartyKindRules

    /** What [party]'s row in a list says. */
    fun summary(party: Party): PartySummary
}
