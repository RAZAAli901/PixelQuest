package com.pixelquest.app.zz

import com.pixelquest.app.darood.domain.RaceLine
import com.pixelquest.app.darood.domain.rankRace
import com.pixelquest.app.darood.ui.race.RaceBoardContent
import com.pixelquest.app.darood.ui.race.RaceBoardUiState
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.ui.DueState
import com.pixelquest.app.study.ui.ItemRow
import com.pixelquest.app.study.ui.StudyItemContent
import com.pixelquest.app.study.ui.StudyPartyUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ItemRaceShotTest : ScratchShot() {
    private val week = LocalDate.of(2026, 10, 19)
    private val party = Party("p1", "Physics group", "aria", 10, null, null, null, Instant.EPOCH, memberCount = 2, myStatus = MemberStatus.ACTIVE, type = PartyType.STUDY)
    private val members = listOf(
        PartyMember("p1", "aria", "aria_k", PartyRole.LEADER, MemberStatus.ACTIVE, null, null),
        PartyMember("p1", "bram", "bram", PartyRole.MEMBER, MemberStatus.ACTIVE, null, null)
    )
    private val quiz = StudyItem("q", "p1", StudyItemKind.QUIZ, "Waves quiz", "Physics", "Pages 4 to 9, and the summary questions at the end.", Instant.parse("2026-10-23T09:00:00Z"), true, "aria", Instant.EPOCH)
    private val tick = StudyCompletion(1, "c", "q", "p1", "bram", Instant.parse("2026-10-21T09:00:00Z"), week, "UTC", SyncState.SYNCED)
    private val state = StudyPartyUiState(
        party = party, myId = "aria", isLeader = true, members = members,
        items = listOf(ItemRow(quiz, null, 1, 2, DueState.SOON, canUndo = false, canEdit = true)),
        ticks = mapOf("q" to listOf(tick)), today = week.plusDays(2), loaded = true
    )

    @Test fun item() = shot("study-item", PqSkin.Comic) { PixelQuestTheme { StudyItemContent(state, "q", onNavigateBack = {}, zone = ZoneId.of("UTC")) } }
    @Test fun itemPixel() = shot("study-item", PqSkin.Pixel) { PixelQuestTheme { StudyItemContent(state, "q", onNavigateBack = {}, zone = ZoneId.of("UTC")) } }

    private fun line(name: String, progress: Float, finished: String? = null, mine: Boolean = false) =
        RaceLine(name, 5, (progress * 1000).toLong(), 1000, progress, finished?.let(Instant::parse), mine)
    private val lines = rankRace(listOf(line("Friday circle", 0.24f, mine = true), line("Early finish", 1f, finished = "2026-10-12T10:00:00Z"), line("Late finish", 1f, finished = "2026-10-15T10:00:00Z"), line("Halfway", 0.5f)))

    @Test fun race() = shot("race", PqSkin.Comic) { PixelQuestTheme { RaceBoardContent(RaceBoardUiState(lines = lines), onNavigateBack = {}) } }
    @Test fun racePixel() = shot("race", PqSkin.Pixel) { PixelQuestTheme { RaceBoardContent(RaceBoardUiState(lines = lines), onNavigateBack = {}) } }
}
