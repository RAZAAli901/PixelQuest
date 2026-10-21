package com.pixelquest.app.party.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The Parties screen: starting a study party, the invitations, the parties, or why to sign in. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class PartiesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun party(id: String, name: String, status: MemberStatus) =
        Party(id, name, "l", 10, null, null, null, Instant.EPOCH, memberCount = 3, myStatus = status, type = PartyType.STUDY)

    private val opened = mutableListOf<String>()

    private fun show(state: DaroodPartiesUiState, mode: ThemeMode = ThemeMode.Pixel) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                PartiesContent(
                    state = state,
                    onSignIn = { opened += "sign in" },
                    onOpenParty = { opened += it.id },
                    onCreateParty = { opened += "create" },
                    onAccept = { opened += "accept $it" }
                )
            }
        }
    }

    @Test
    fun signedIn_itOffersToStartOne_thenItsInvitationsAndParties() {
        show(DaroodPartiesUiState(signedIn = true, parties = listOf(party("s1", "Physics group", MemberStatus.ACTIVE)), invites = listOf(party("s2", "Exam group", MemberStatus.INVITED))), ThemeMode.Comic)

        composeTestRule.onNodeWithTag(PartiesTags.CREATE).performClick()
        composeTestRule.onNodeWithText("INVITATIONS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Study · 3 members").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.partyRow("s1")).performClick()
        composeTestRule.onNodeWithTag(DaroodTestTags.inviteRow("s2")).assertIsDisplayed()

        assertEquals(listOf("create", "s1"), opened)
    }

    @Test
    fun withNone_itSaysWhatPartiesAre_andOffersToStartOne() {
        show(DaroodPartiesUiState(signedIn = true), ThemeMode.Light)

        composeTestRule.onNodeWithText("PARTIES").assertIsDisplayed()
        composeTestRule.onNodeWithTag(PartiesTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("NO PARTIES YET").assertIsDisplayed()
        composeTestRule.onNodeWithText("TWO KINDS OF PARTY").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE).performClick()
        assertEquals(listOf("create"), opened)
    }

    @Test
    fun everyKindsInvitation_arrivesHere_sayingItsKind() {
        val darood = party("d1", "Friday circle", MemberStatus.INVITED).copy(type = PartyType.DAROOD)
        val study = party("s2", "Exam group", MemberStatus.INVITED)
        composeTestRule.setContent {
            PixelQuestTheme {
                PartiesContent(state = DaroodPartiesUiState(signedIn = true, invites = listOf(darood, study)), onSignIn = {}, onOpenParty = {}, onCreateParty = {})
            }
        }

        composeTestRule.onNodeWithText("You're invited to a Darood Party", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("You're invited to a Study party", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun signedOut_itAsksToSignIn() {
        show(DaroodPartiesUiState(signedIn = false))

        composeTestRule.onNodeWithTag(PartiesTags.SIGN_IN).performClick()
        composeTestRule.onNodeWithTag(PartiesTags.CREATE).assertDoesNotExist()
        assertEquals(listOf("sign in"), opened)
    }
}
