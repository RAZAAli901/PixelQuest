package com.pixelquest.app.darood.ui

import com.pixelquest.app.R
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyKind
import com.pixelquest.app.party.domain.PartyKindRules
import com.pixelquest.app.party.domain.PartySummary
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.domain.Phrase

/**
 * The Darood Party: its members post the salawat they recite, toward a milestone they race to,
 * and may join the race board.
 */
object DaroodPartyKind : PartyKind {
    override val type = PartyType.DAROOD
    override val titleRes = R.string.darood_title
    override val icon = "📿"
    override val tabs = listOf(PartyTab.CHAT, PartyTab.PEOPLE)
    override val rules = PartyKindRules(milestone = true, raceBoard = true)

    /** Its members and total; then how far it is to its milestone, or that it has none yet. */
    override fun summary(party: Party): PartySummary {
        val milestone = party.milestone
        return PartySummary(
            main = listOf(
                Phrase.Plural(R.plurals.darood_members, party.memberCount),
                Phrase.Text(R.string.darood_total, listOf(formatCount(party.total)))
            ),
            detail = if (milestone != null) {
                Phrase.Text(R.string.darood_progress, listOf(formatCount(party.total), formatCount(milestone)))
            } else {
                Phrase.Text(R.string.darood_no_milestone)
            },
            progress = if (milestone != null) party.progress ?: 0f else null
        )
    }
}
