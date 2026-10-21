package com.pixelquest.app.darood.ui.list

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.ui.PartiesContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * The Parties hub (UI v2, mocks 4B and 4C): each party's kind, size, total and progress, opening a
 * party, the invitations, the filter by kind, and the weekly note.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PartyListUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun party(id: String, name: String, members: Int, total: Long, milestone: Long?, finished: Boolean = false, type: PartyType = PartyType.DAROOD) = Party(
        id = id, name = name, leaderId = "me", maxMembers = 10, milestone = milestone, milestoneSetAt = null,
        completedAt = if (finished) Instant.EPOCH else null, createdAt = Instant.EPOCH, total = total, memberCount = members,
        myStatus = MemberStatus.ACTIVE, type = type
    )

    @Test
    fun eachParty_showsItsKindSize_totalAndProgress_andOpens() {
        var opened: String? = null
        val state = DaroodPartiesUiState(
            signedIn = true,
            parties = listOf(
                party("p1", "Friday circle", members = 12, total = 2450, milestone = 10_000),
                party("p2", "Family", members = 1, total = 0, milestone = null),
                party("p3", "Ramadan", members = 5, total = 5000, milestone = 5000, finished = true)
            )
        )
        composeTestRule.setContent {
            PartiesContent(state = state, onSignIn = {}, onNavigateBack = {}, onOpenParty = { opened = it.id }, onCreateParty = {})
        }

        composeTestRule.onNodeWithText("Friday circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Darood · 12 members").assertIsDisplayed()
        composeTestRule.onNodeWithText("2,450 of 10,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Darood · 1 member").assertIsDisplayed()
        composeTestRule.onNodeWithText("No milestone yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("FINISHED").assertIsDisplayed()

        composeTestRule.onNodeWithTag(DaroodTestTags.partyRow("p1")).performClick()
        assertEquals("p1", opened)
    }

    @Test
    fun bothKinds_canBeFiltered() {
        val state = DaroodPartiesUiState(
            signedIn = true,
            parties = listOf(party("p1", "Friday circle", 3, 10, null), party("s1", "Exam group", 4, 0, null, type = PartyType.STUDY))
        )
        composeTestRule.setContent { PartiesContent(state = state, onSignIn = {}, onOpenParty = {}, onCreateParty = {}) }

        composeTestRule.onNodeWithText("All · 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Study · 4 members").assertIsDisplayed()
        composeTestRule.onNodeWithText("Study").performClick()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag(DaroodTestTags.partyRow("p1"))).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag(DaroodTestTags.partyRow("s1")).assertIsDisplayed()
    }

    @Test
    fun offline_itSaysTheListIsThePhones() {
        composeTestRule.setContent {
            PartiesContent(
                state = DaroodPartiesUiState(signedIn = true, parties = listOf(party("p1", "Friday circle", 2, 10, null)), problem = PartiesProblem.OFFLINE),
                onSignIn = {}, onOpenParty = {}, onCreateParty = {}
            )
        }
        composeTestRule.onNodeWithText("Couldn't reach the server. Showing what this phone has.").assertIsDisplayed()
    }

    @Test
    fun noParties_invitesToCreateOne() {
        var created = 0
        composeTestRule.setContent {
            PartiesContent(state = DaroodPartiesUiState(signedIn = true), onSignIn = {}, onOpenParty = {}, onCreateParty = { created++ }, username = "raza")
        }

        composeTestRule.onNodeWithText("NO PARTIES YET").assertIsDisplayed()
        composeTestRule.onNodeWithText("SHARE @RAZA").assertIsDisplayed()
        // The empty hub's own button is the one red button: none in the top bar.
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag(DaroodTestTags.CREATE_ACTION)).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE).performClick()
        assertEquals(1, created)
    }

    @Test
    fun withParties_thePlusStartsOne() {
        var created = 0
        composeTestRule.setContent {
            PartiesContent(state = DaroodPartiesUiState(signedIn = true, parties = listOf(party("p1", "Friday circle", 2, 10, null))), onSignIn = {}, onOpenParty = {}, onCreateParty = { created++ })
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_ACTION).performClick()
        assertEquals(1, created)
    }

    @Test
    fun invitations_comeFirst_withAcceptAndDecline() {
        val accepted = mutableListOf<String>()
        val declined = mutableListOf<String>()
        val invite = party("p9", "Family", members = 0, total = 0, milestone = null).copy(myStatus = MemberStatus.INVITED)
        composeTestRule.setContent {
            PartiesContent(
                state = DaroodPartiesUiState(signedIn = true, invites = listOf(invite), parties = listOf(party("p1", "Friday circle", 2, 10, null))),
                onSignIn = {}, onOpenParty = {}, onCreateParty = {},
                onAccept = { accepted += it },
                onDecline = { declined += it }
            )
        }

        composeTestRule.onNodeWithText("INVITATIONS").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.inviteAccept("p9")).performClick()
        composeTestRule.onNodeWithTag(DaroodTestTags.inviteDecline("p9")).performClick()
        assertEquals(listOf("p9"), accepted)
        assertEquals(listOf("p9"), declined)
    }

    @Test
    fun whileAnswering_theButtonsWait_andAGoneInvitationIsExplained() {
        val invite = party("p9", "Family", members = 0, total = 0, milestone = null).copy(myStatus = MemberStatus.INVITED)
        composeTestRule.setContent {
            PartiesContent(
                state = DaroodPartiesUiState(signedIn = true, invites = listOf(invite), answering = "p9", problem = PartiesProblem.INVITE_GONE),
                onSignIn = {}, onOpenParty = {}, onCreateParty = {}
            )
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.inviteAccept("p9")).assertIsNotEnabled()
        composeTestRule.onNodeWithText("That invitation isn't there any more: it may have been withdrawn, or the party is full or finished.").assertIsDisplayed()
    }

    @Test
    fun theWeeklyNote_isOffUnlessTurnedOn_andSaysPlainlyWhatItIs() {
        var turned: Boolean? = null
        composeTestRule.setContent {
            PartiesContent(
                state = DaroodPartiesUiState(signedIn = true, parties = listOf(party("p1", "Friday circle", 2, 10, null))),
                onSignIn = {}, onOpenParty = {}, onCreateParty = {},
                weeklyNote = false, onWeeklyNote = { turned = it }
            )
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.WEEKLY_SWITCH).performScrollTo().assertIsOff()
        composeTestRule.onNodeWithText("Off unless you turn it on.", substring = true).assertExists()
        composeTestRule.onNodeWithTag(DaroodTestTags.WEEKLY_SWITCH).performClick()
        assertEquals(true, turned)
    }

    @Test
    fun withoutDarood_noWeeklyNote_andNoRaceBoard() {
        composeTestRule.setContent {
            PartiesContent(
                state = DaroodPartiesUiState(signedIn = true, parties = listOf(party("s1", "Exam group", 4, 0, null, type = PartyType.STUDY))),
                onSignIn = {}, onOpenParty = {}, onCreateParty = {}, daroodAvailable = false
            )
        }
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag(DaroodTestTags.WEEKLY_SWITCH)).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag(DaroodTestTags.RACE_ACTION)).fetchSemanticsNodes().size)
    }
}
