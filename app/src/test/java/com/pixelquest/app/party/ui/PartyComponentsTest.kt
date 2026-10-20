package com.pixelquest.app.party.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.invite.Lookup
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.ui.components.InviteSearch
import com.pixelquest.app.party.ui.components.LookupResult
import com.pixelquest.app.party.ui.components.MemberLimitStepper
import com.pixelquest.app.party.ui.components.MemberList
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The party screens' shared pieces, on their own: any kind of party's screen can use them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class PartyComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theMemberLimit_stopsAtItsEnds() {
        var asked = ""
        composeTestRule.setContent {
            PixelQuestTheme { MemberLimitStepper(count = 2, canFewer = false, canMore = true, onFewer = { asked += "-" }, onMore = { asked += "+" }) }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_FEWER).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_MORE).assertIsEnabled().performClick()
        assertEquals("+", asked)
    }

    @Test
    fun theInviteSearch_offersWhoItFound() {
        var invited: String? = null
        composeTestRule.setContent {
            PixelQuestTheme {
                Column {
                    InviteSearch(query = "sam_w", onQueryChange = {})
                    LookupResult(Lookup.Found(Username.parse("sam_w")!!), enabled = true, onInvite = { invited = it })
                }
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.ADD_INVITE).performClick()
        assertEquals("sam_w", invited)
    }

    @Test
    fun theMemberList_givesTheLeaderToolsForOthers_neverThemselves() {
        val members = listOf(
            PartyMember("p", "me", "aria_k", PartyRole.LEADER, MemberStatus.ACTIVE, null, null),
            PartyMember("p", "sam", "sam_w", PartyRole.MEMBER, MemberStatus.ACTIVE, null, null)
        )
        val removed = mutableListOf<String>()
        composeTestRule.setContent {
            PixelQuestTheme { MemberList(members, myId = "me", isLeader = true, enabled = true, onRemove = { removed += it.userId }, onTransfer = {}) }
        }
        composeTestRule.onNodeWithText("@aria_k (you)", substring = true).assertExists()
        composeTestRule.onNodeWithTag(DaroodTestTags.memberRemove("me")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.memberRemove("sam")).performClick()
        assertEquals(listOf("sam"), removed)
    }
}
