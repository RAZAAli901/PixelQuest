package com.pixelquest.app.zz

import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.darood.ui.invite.AddPeopleContent
import com.pixelquest.app.darood.ui.invite.AddPeopleUiState
import com.pixelquest.app.darood.ui.invite.InviteResult
import com.pixelquest.app.darood.ui.invite.Lookup
import com.pixelquest.app.darood.ui.username.UsernameAvailability
import com.pixelquest.app.darood.ui.username.UsernameSetupContent
import com.pixelquest.app.darood.ui.username.UsernameSetupUiState
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class InviteShotTest : ScratchShot() {
    private val people = listOf(
        PartyMember("p", "aria-id", "aria_k", PartyRole.LEADER, MemberStatus.ACTIVE, null, null),
        PartyMember("p", "bram-id", "bram", PartyRole.MEMBER, MemberStatus.INVITED, null, null),
        PartyMember("p", "cleo-id", "cleo_r", PartyRole.MEMBER, MemberStatus.DECLINED, null, null)
    )
    private val add = AddPeopleUiState(query = "sam_w", lookup = Lookup.Found(Username.parse("sam_w")!!), people = people, result = InviteResult.Sent("bram"))

    @Test fun add() = shot("add-people", PqSkin.Comic) { PixelQuestTheme { AddPeopleContent(add, {}, {}) } }
    @Test fun addPixel() = shot("add-people", PqSkin.Pixel) { PixelQuestTheme { AddPeopleContent(add, {}, {}) } }

    private val name = UsernameSetupUiState(input = "aria_k", availability = UsernameAvailability.AVAILABLE, loadingCurrent = false)
    @Test fun username() = shot("username", PqSkin.Comic) { PixelQuestTheme { UsernameSetupContent(name, {}, {}, {}) } }
    @Test fun usernamePixel() = shot("username", PqSkin.Pixel) { PixelQuestTheme { UsernameSetupContent(name, {}, {}, {}) } }
}
