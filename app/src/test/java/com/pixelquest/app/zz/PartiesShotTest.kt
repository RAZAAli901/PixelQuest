package com.pixelquest.app.zz

import com.pixelquest.app.darood.ui.create.CreatePartyContent
import com.pixelquest.app.darood.ui.create.CreatePartyUiState
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.ui.PartiesContent
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test
import java.time.Instant

class PartiesShotTest : ScratchShot() {
    private fun party(id: String, name: String, members: Int, total: Long, milestone: Long?, type: PartyType = PartyType.DAROOD, status: MemberStatus = MemberStatus.ACTIVE) = Party(
        id, name, "me", 10, milestone, null, null, Instant.EPOCH, total = total, memberCount = members, myStatus = status, type = type)
    private val state = DaroodPartiesUiState(signedIn = true,
        parties = listOf(party("1", "Raza", 1, 726, 10_000), party("2", "Bros", 3, 0, null, PartyType.STUDY)),
        invites = listOf(party("3", "Exam group", 4, 0, null, PartyType.STUDY, MemberStatus.INVITED)))
    @Test fun hub() = shot("parties", PqSkin.Comic) { PartiesContent(state, {}, {}, {}, onNavigateBack = {}, username = "raza") }
    @Test fun hubPixel() = shot("parties", PqSkin.Pixel) { PartiesContent(state.copy(invites = emptyList()), {}, {}, {}, onNavigateBack = {}, username = "raza") }
    @Test fun empty() = shot("parties-empty", PqSkin.Comic) { PartiesContent(DaroodPartiesUiState(signedIn = true), {}, {}, {}, onNavigateBack = {}, username = "raza") }
    @Test fun create() = shot("newparty", PqSkin.Comic) { CreatePartyContent(CreatePartyUiState(name = "Friday circle", milestoneText = "100000"), {}, {}) }
}
