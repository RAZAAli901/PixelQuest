package com.pixelquest.app.study.ui

import com.pixelquest.app.R
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyKind
import com.pixelquest.app.party.domain.PartyKindRules
import com.pixelquest.app.party.domain.PartySummary
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.domain.Phrase

/**
 * The Study party: friends who pin their assignments and quizzes and keep each other going, with
 * points of its own, never XP. Its items, scores and tabs' contents come with Phase E; for now it's
 * the shared core, its members.
 */
object StudyPartyKind : PartyKind {
    override val type = PartyType.STUDY
    override val titleRes = R.string.party_study_title
    override val listTitleRes = R.string.party_study_list_title
    override val emptyBodyRes = R.string.party_study_empty_body
    override val signedOutBodyRes = R.string.party_signed_out_body
    override val handOverRes = R.string.party_hand_over
    override val icon = "📚"
    override val tabs = listOf(PartyTab.THIS_WEEK, PartyTab.ALL_ITEMS, PartyTab.SCOREBOARD, PartyTab.PEOPLE)
    override val rules = PartyKindRules(milestone = false, raceBoard = false, weeklyNote = false)

    /** Its members. */
    override fun summary(party: Party) = PartySummary(main = listOf(Phrase.Plural(R.plurals.party_members, party.memberCount)))
}
