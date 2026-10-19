package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyRole
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The party's board: by contribution, kindly. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PartyBoardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun stat(user: String, total: Long) = MemberStats("p1", user, user.removeSuffix("-id"), total, 1, null)

    private fun member(user: String, status: MemberStatus = MemberStatus.ACTIVE) =
        PartyMember("p1", user, user.removeSuffix("-id"), PartyRole.MEMBER, status, null, null)

    private val stats = listOf(stat("bram-id", 2_450), stat("cleo-id", 5_000), stat("aria-id", 2_450), stat("dara-id", 100), stat("gone-id", 40))
    private val members = listOf("aria-id", "bram-id", "cleo-id", "dara-id", "eli-id", "fay-id").map { member(it) } + member("gone-id", MemberStatus.LEFT)

    @Test
    fun mostFirst_levelTotalsShareAPlace_andMedalsGoToTheTopThreePlacesOnly() {
        val board = partyBoardOf(stats, members, me = "aria-id")
        assertEquals(listOf(5_000L, 2_450L, 2_450L, 100L, 40L), board.rows.map { it.total })
        assertEquals(listOf(1, 2, 2, 4, 5), board.rows.map { it.place })
        assertEquals(listOf("🥇", "🥈", "🥈", null, null), board.rows.map { it.medal })
        assertEquals("the player's own row is marked", "aria-id", board.rows.single { it.mine }.userId)
        assertEquals("someone who left still has their part", 40L, board.rows.last().total)
    }

    @Test
    fun activeMembersYetToAdd_areNamedApart_notRankedAtNothing() {
        val board = partyBoardOf(stats, members, me = "aria-id")
        assertEquals(listOf("eli", "fay"), board.yetToAdd)
        assertEquals("the player isn't told they're yet to add", emptyList<String>(), partyBoardOf(emptyList(), listOf(member("aria-id")), me = "aria-id").yetToAdd)
    }

    @Test
    fun onScreen_noPlaceNumbers_andAKindLineForThoseYetToAdd() {
        val board = partyBoardOf(stats, members, me = "aria-id")
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) { PartyBoardSection(board) }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.boardRow("cleo-id")).assertContentDescriptionEquals("🥇 cleo, 5,000")
        composeTestRule.onNodeWithTag(DaroodTestTags.boardRow("aria-id")).assertContentDescriptionEquals("🥈 aria (you), 2,450")
        composeTestRule.onNodeWithTag(DaroodTestTags.boardRow("dara-id")).assertContentDescriptionEquals("dara, 100")
        composeTestRule.onNodeWithText("Still to add their first: eli, fay").assertExists()
        // Never "4th", "5th" or "last".
        for (word in listOf("4", "5th", "last")) composeTestRule.onNodeWithText(word, substring = false).assertDoesNotExist()
    }
}
