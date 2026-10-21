package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.MessageBubbleRow
import com.pixelquest.app.study.chat.ui.MessageMenuItems
import com.pixelquest.app.study.chat.ui.menuItemsFor
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** A message's long-press menu: what each message offers, and to whom. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class MessageMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun row(mine: Boolean, deleted: Boolean = false, sync: SyncState? = null) = ChatMessageRow(
        ChatMessage("m", "p1", if (mine) "aria" else "bram", ChatMessageKind.TEXT, if (deleted) null else "Hi", null, null, null, Instant.EPOCH, if (deleted) Instant.EPOCH else null, sync),
        mine = mine, senderName = null
    )

    @Test
    fun eachMessage_offersWhatsItsToOffer() {
        assertEquals("the player's own", MessageMenuItems(copy = true, reply = true, delete = true, report = false), menuItemsFor(row(mine = true), isLeader = false, canReport = true))
        assertEquals("someone else's", MessageMenuItems(copy = true, reply = true, delete = false, report = true), menuItemsFor(row(mine = false), isLeader = false, canReport = true))
        assertEquals("someone else's, to the leader", MessageMenuItems(copy = true, reply = true, delete = true, report = true), menuItemsFor(row(mine = false), isLeader = true, canReport = true))
        assertEquals("no way to report yet", false, menuItemsFor(row(mine = false), isLeader = false, canReport = false).report)
        assertEquals("deleted: nothing", MessageMenuItems(false, false, false, false), menuItemsFor(row(mine = false, deleted = true), isLeader = true, canReport = true))
        assertEquals("not sent yet: nothing", MessageMenuItems(false, false, false, false), menuItemsFor(row(mine = true, sync = SyncState.PENDING), isLeader = true, canReport = true))
    }

    @Test
    fun aLongPress_opensTheMenu() {
        var opened = 0
        composeTestRule.setContent { PixelQuestTheme { MessageBubbleRow(row(mine = false), onMenu = { opened++ }) } }
        composeTestRule.onNodeWithText("Hi").performTouchInput { longClick() }
        assertEquals(1, opened)
    }
}
