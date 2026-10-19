package com.pixelquest.app.darood.ui.list

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.ui.DaroodPartyContent
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The party list: each row's name, size, total and progress, and opening a party. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PartyListUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun party(id: String, name: String, members: Int, total: Long, milestone: Long?, finished: Boolean = false) = Party(
        id = id, name = name, leaderId = "me", maxMembers = 10, milestone = milestone, milestoneSetAt = null,
        completedAt = if (finished) Instant.EPOCH else null, createdAt = Instant.EPOCH, total = total, memberCount = members,
        myStatus = MemberStatus.ACTIVE
    )

    @Test
    fun eachParty_showsItsSize_totalAndProgress_andOpens() {
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
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                DaroodPartyContent(state = state, onSignIn = {}, onNavigateBack = {}, onOpenParty = { opened = it })
            }
        }

        composeTestRule.onNodeWithText("Friday circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 members  ·  Total 2,450").assertIsDisplayed()
        composeTestRule.onNodeWithText("2,450 of 10,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 member  ·  Total 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("No milestone yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finished").assertIsDisplayed()

        composeTestRule.onNodeWithTag(DaroodTestTags.partyRow("p1")).performClick()
        assertEquals("p1", opened)
    }

    @Test
    fun offline_itSaysTheListIsThePhones() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                DaroodPartyContent(
                    state = DaroodPartiesUiState(signedIn = true, parties = listOf(party("p1", "Friday circle", 2, 10, null)), problem = PartiesProblem.OFFLINE),
                    onSignIn = {},
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Couldn't reach the server. Showing what this phone has.").assertIsDisplayed()
    }

    @Test
    fun noParties_invitesToCreateOne_andThePlusDoesToo() {
        var created = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Light, darkTheme = false) {
                DaroodPartyContent(
                    state = DaroodPartiesUiState(signedIn = true),
                    onSignIn = {},
                    onNavigateBack = {},
                    onCreateParty = { created++ }
                )
            }
        }

        composeTestRule.onNodeWithText("No parties yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE).performClick()
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_ACTION).performClick()
        assertEquals(2, created)
    }
}
