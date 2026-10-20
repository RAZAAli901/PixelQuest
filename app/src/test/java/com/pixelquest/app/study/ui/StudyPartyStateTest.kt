package com.pixelquest.app.study.ui

import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.domain.WeekScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the study party's screen makes of its data: due states, cards, this week, and the board. */
class StudyPartyStateTest {

    private val zone = ZoneId.of("Asia/Karachi")
    private val now = Instant.parse("2026-10-21T10:00:00Z") // Wednesday, 15:00 in Karachi
    private val week = LocalDate.of(2026, 10, 19)

    private fun member(id: String, name: String? = id, status: MemberStatus = MemberStatus.ACTIVE) =
        PartyMember("p", id, name, if (id == "aria") PartyRole.LEADER else PartyRole.MEMBER, status, null, null)

    private fun item(id: String, dueAt: Instant?, by: String = "bram") =
        StudyItem(id, "p", StudyItemKind.ASSIGNMENT, "Item $id", null, null, dueAt, false, by, now)

    private fun tick(item: String, user: String, at: Instant = now, state: SyncState = SyncState.SYNCED) =
        StudyCompletion(if (state == SyncState.SYNCED) 1 else null, "c-$item-$user", item, "p", user, at, week.plusDays(2), zone.id, state)

    @Test
    fun dueStates() {
        assertEquals(DueState.NONE, dueStateOf(null, now, zone))
        assertEquals(DueState.OVERDUE, dueStateOf(now.minusSeconds(1), now, zone))
        assertEquals(DueState.TODAY, dueStateOf(Instant.parse("2026-10-21T18:00:00Z"), now, zone))
        assertEquals("tomorrow in Karachi", DueState.SOON, dueStateOf(Instant.parse("2026-10-21T19:30:00Z"), now, zone))
        assertEquals(DueState.LATER, dueStateOf(now.plus(Duration.ofDays(4)), now, zone))
    }

    @Test
    fun aCard_countsActiveMembersDone_andKnowsWhatThePlayerMayDo() {
        val members = listOf(member("aria"), member("bram"), member("cleo"), member("dan", status = MemberStatus.LEFT))
        val items = listOf(item("a", now.minus(Duration.ofHours(1))), item("b", null, by = "aria"))
        val ticks = listOf(tick("a", "bram"), tick("a", "dan"), tick("b", "cleo", at = now.minus(Duration.ofHours(30))))

        val asBram = itemRowsOf(items, ticks, members, "bram", isLeader = false, now, zone).associateBy { it.item.id }
        assertEquals(1, asBram.getValue("a").doneCount)
        assertEquals(3, asBram.getValue("a").memberCount)
        assertTrue(asBram.getValue("a").done)
        assertTrue(asBram.getValue("a").canUndo)
        assertEquals("done, so no longer overdue for him", DueState.LATER, asBram.getValue("a").due)
        assertTrue(asBram.getValue("a").canEdit)
        assertFalse(asBram.getValue("b").canEdit)

        val asCleo = itemRowsOf(items, ticks, members, "cleo", isLeader = false, now, zone).associateBy { it.item.id }
        assertEquals(DueState.OVERDUE, asCleo.getValue("a").due)
        assertFalse("past 24 hours", asCleo.getValue("b").canUndo)

        val asAria = itemRowsOf(items, ticks, members, "aria", isLeader = true, now, zone)
        assertTrue(asAria.all { it.canEdit })
    }

    @Test
    fun aRefusedTick_isntDone_butAWaitingOneIs() {
        val members = listOf(member("aria"))
        val rows = itemRowsOf(listOf(item("a", null), item("b", null)), listOf(tick("a", "aria", state = SyncState.FAILED), tick("b", "aria", state = SyncState.PENDING)), members, "aria", false, now, zone)
        assertFalse(rows[0].done)
        assertEquals(0, rows[0].doneCount)
        assertTrue(rows[1].done)
        assertTrue(rows[1].canUndo)
    }

    @Test
    fun thisWeek_isDueThisWeek_plusWhatsStillToDo() {
        val members = listOf(member("aria"))
        val items = listOf(
            item("thisWeek", Instant.parse("2026-10-25T12:00:00Z")),
            item("nextWeek", Instant.parse("2026-10-27T12:00:00Z")),
            item("overdue", Instant.parse("2026-10-14T12:00:00Z")),
            item("overdueDone", Instant.parse("2026-10-14T12:00:00Z")),
            item("undated", null),
            item("undatedDone", null)
        )
        val rows = itemRowsOf(items, listOf(tick("overdueDone", "aria"), tick("undatedDone", "aria")), members, "aria", false, now, zone)
        assertEquals(listOf("thisWeek", "overdue", "undated"), thisWeekOf(rows, week, zone).map { it.item.id })
    }

    @Test
    fun thisWeek_isOverdueFirst_thenEachDay_thenNoDate() {
        val members = listOf(member("aria"))
        val items = listOf(
            item("fri", Instant.parse("2026-10-23T12:00:00Z")),
            item("undated", null),
            item("thuLate", Instant.parse("2026-10-22T15:00:00Z")),
            item("overdue", Instant.parse("2026-10-20T12:00:00Z")),
            item("thuEarly", Instant.parse("2026-10-22T05:00:00Z"))
        )
        val rows = itemRowsOf(items, listOf(tick("fri", "aria")), members, "aria", false, now, zone)
        val sections = weekSectionsOf(rows, zone)
        assertEquals(
            listOf(WeekSection.Overdue, WeekSection.Day(LocalDate.of(2026, 10, 22)), WeekSection.Day(LocalDate.of(2026, 10, 23)), WeekSection.NoDate),
            sections.map { it.first }
        )
        assertEquals(listOf("thuEarly", "thuLate"), sections[1].second.map { it.item.id })
        assertEquals(WeekProgress(1, 5), weekProgressOf(rows))
    }

    @Test
    fun pastWeeks_newestFirst_withWinners_andTheBannerUntilSeen() {
        fun score(user: String, week: LocalDate, points: Int) = WeekScore("p", user, null, week, points, points, 0, 0, 1, 0)
        val last = week.minusWeeks(1)
        val before = week.minusWeeks(2)
        val winners = listOf(
            com.pixelquest.app.study.domain.WeekWinner("p", last, "bram", null, 40),
            com.pixelquest.app.study.domain.WeekWinner("p", last, "cleo", null, 40)
        )
        val past = pastWeeksOf(listOf(score("aria", before, 10), score("aria", last, 25), score("bram", last, 40)), winners, "aria")
        assertEquals(listOf(last, before), past.map { it.weekStart })
        assertEquals(25, past[0].myPoints)
        assertEquals(listOf("bram", "cleo"), past[0].winners.map { it.userId })
        assertTrue(past[1].winners.isEmpty())

        assertEquals(winners, winnerBannerOf(winners, week, seenWeek = null))
        assertEquals(winners, winnerBannerOf(winners, week, seenWeek = before))
        assertTrue(winnerBannerOf(winners, week, seenWeek = last).isEmpty())
        assertTrue("an older week's winners aren't news", winnerBannerOf(winners, week.plusWeeks(1), seenWeek = null).isEmpty())
    }

    @Test
    fun theBoard_hasEveryone_andEqualPointsShareAPlace() {
        fun score(user: String, points: Int) = WeekScore("p", user, null, week, points, points, 0, 0, 1, 0)
        val members = listOf(member("aria", "aria_k"), member("bram"), member("cleo"), member("dan"), member("eve", status = MemberStatus.INVITED))
        val board = boardOf(listOf(score("bram", 25), score("cleo", 25), score("aria", 10)), members, "aria")
        assertEquals(listOf("bram" to 1, "cleo" to 1, "aria" to 3, "dan" to 4), board.map { it.userId to it.place })
        assertEquals(0, board.last().points)
        assertTrue(board.single { it.isMe }.userId == "aria")
    }
}
