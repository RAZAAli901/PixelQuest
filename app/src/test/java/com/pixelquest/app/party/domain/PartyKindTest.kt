package com.pixelquest.app.party.domain

import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodPartyKind
import com.pixelquest.app.party.PartyKinds
import com.pixelquest.app.study.ui.StudyPartyKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Each kind of party: its type on the wire, its own rules and tabs, and what its list rows say. */
class PartyKindTest {

    private fun party(milestone: Long? = null, total: Long = 0) =
        Party("p", "Friday circle", "l", 10, milestone, null, null, Instant.EPOCH, total = total, memberCount = 4)

    @Test
    fun aTypeIsReadFromTheWire_andAnUnknownOneIsDarood() {
        assertEquals(PartyType.STUDY, PartyType.fromWire("study"))
        assertEquals(PartyType.DAROOD, PartyType.fromWire("darood"))
        assertEquals("as every party before kinds", PartyType.DAROOD, PartyType.fromWire(null))
        assertEquals(PartyType.DAROOD, PartyType.fromWire("chess"))
        assertEquals(listOf("darood", "study"), PartyType.entries.map { it.wire })
    }

    @Test
    fun everyType_hasItsKind() {
        for (type in PartyType.entries) assertEquals(type, PartyKinds.of(type).type)
        assertEquals(DaroodPartyKind, PartyKinds.of(PartyType.DAROOD))
        assertEquals(StudyPartyKind, PartyKinds.of(PartyType.STUDY))
    }

    @Test
    fun onlyDarood_hasAMilestoneAndTheRaceBoard() {
        assertTrue(DaroodPartyKind.rules.milestone && DaroodPartyKind.rules.raceBoard)
        assertFalse(StudyPartyKind.rules.milestone || StudyPartyKind.rules.raceBoard)
        assertEquals(PartyTab.PEOPLE, DaroodPartyKind.tabs.last())
        assertEquals(listOf(PartyTab.THIS_WEEK, PartyTab.ALL_ITEMS, PartyTab.CHESS, PartyTab.SCOREBOARD, PartyTab.CHAT, PartyTab.FILES, PartyTab.PEOPLE), StudyPartyKind.tabs)
    }

    @Test
    fun aDaroodRow_saysItsMembersAndTotal_thenItsMilestone() {
        val racing = DaroodPartyKind.summary(party(milestone = 10_000, total = 2_450))
        assertEquals(Phrase.Plural(R.plurals.darood_members, 4), racing.main.first())
        assertEquals(Phrase.Text(R.string.darood_progress, listOf("2,450", "10,000")), racing.detail)
        assertEquals(0.245f, racing.progress!!, 0.0001f)

        val open = DaroodPartyKind.summary(party())
        assertEquals(Phrase.Text(R.string.darood_no_milestone), open.detail)
        assertNull("no bar without a milestone", open.progress)
    }

    @Test
    fun aStudyRow_saysItsMembers() {
        val summary = StudyPartyKind.summary(party())
        assertEquals(listOf(Phrase.Plural(R.plurals.party_members, 4)), summary.main)
        assertNull(summary.detail)
        assertNull(summary.progress)
    }
}
