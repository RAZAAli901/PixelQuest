package com.pixelquest.app.party.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.create.CreatePartyScreen
import com.pixelquest.app.darood.ui.create.CreatePartyViewModel
import com.pixelquest.app.darood.ui.info.PartyInfoContent
import com.pixelquest.app.darood.ui.info.PartyInfoUiState
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.ui.StudyPartyKind
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * The shared party screens for a study party: what's Darood's own (the milestone, the race board,
 * the weekly note) isn't there, and the texts are the study party's. Darood's own tests keep theirs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class TypeAwareScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val db = androidx.room.Room.inMemoryDatabaseBuilder(
        androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>(),
        com.pixelquest.app.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "me").apply { usernames["me"] = "aria_k" }
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })

    private val study = Party("s1", "Physics group", "me", 10, null, null, null, Instant.EPOCH, memberCount = 3, myStatus = MemberStatus.ACTIVE, type = PartyType.STUDY)

    @After
    fun closeDb() {
        db.close()
    }

    private fun gone(tag: String) = composeTestRule.onNodeWithTag(tag).assertDoesNotExist()

    /** Waits as the Darood screen tests do: Room answers on its own threads, then the main looper runs. */
    private fun waitFor(condition: () -> Boolean) {
        repeat(250) {
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("still waiting")
    }

    @Test
    fun startingAStudyParty_asksNoMilestone_andStartsOne() {
        var opened: String? = null
        val viewModel = CreatePartyViewModel(repository, SavedStateHandle(mapOf(CreatePartyViewModel.ARG_TYPE to "study")))
        composeTestRule.setContent {
            PixelQuestTheme { CreatePartyScreen(onNavigateBack = {}, onCreated = { id, _ -> opened = id }, viewModel = viewModel) }
        }
        gone(DaroodTestTags.CREATE_MILESTONE)

        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.CREATE_NAME))).performTextInput("Physics group")
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_BUTTON).performClick()
        waitFor { opened != null }

        assertEquals("study", server.parties.getValue(opened!!).partyType)
        assertEquals(null, server.parties.getValue(opened!!).milestone)
    }

    @Test
    fun startingWithoutAKind_isADaroodParty_withItsMilestone() {
        composeTestRule.setContent {
            PixelQuestTheme { CreatePartyScreen(onNavigateBack = {}, onCreated = { _, _ -> }, viewModel = CreatePartyViewModel(repository)) }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_MILESTONE).assertIsDisplayed()
    }

    @Test
    fun aStudyPartysInfo_hasNoMilestoneOrRaceBoard() {
        val members = listOf(PartyMember("s1", "me", "aria_k", PartyRole.LEADER, MemberStatus.ACTIVE, null, null))
        composeTestRule.setContent {
            PixelQuestTheme { PartyInfoContent(state = PartyInfoUiState(party = study, members = members, myId = "me"), onNavigateBack = {}) }
        }
        composeTestRule.onNodeWithText("PHYSICS GROUP").assertIsDisplayed()
        gone(DaroodTestTags.INFO_MILESTONE)
        gone(DaroodTestTags.INFO_SET_MILESTONE)
        gone(DaroodTestTags.INFO_RACE_SWITCH)
        // The leader's own buttons are there, as in any party.
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_ADD_PEOPLE).assertIsDisplayed()
    }

    @Test
    fun aStudyPartysCard_saysItsKind_withNoMilestone_andNoWeeklyNoteWithoutDarood() {
        composeTestRule.setContent {
            PixelQuestTheme {
                PartiesContent(
                    state = DaroodPartiesUiState(signedIn = true, parties = listOf(study)),
                    onSignIn = {},
                    onOpenParty = {},
                    onCreateParty = {},
                    weeklyNote = false
                )
            }
        }
        composeTestRule.onNodeWithText("Study · 3 members", useUnmergedTree = true).assertIsDisplayed()
        gone(DaroodTestTags.WEEKLY_SWITCH)
        composeTestRule.onNodeWithText("No milestone yet", useUnmergedTree = true).assertDoesNotExist()
    }
}
