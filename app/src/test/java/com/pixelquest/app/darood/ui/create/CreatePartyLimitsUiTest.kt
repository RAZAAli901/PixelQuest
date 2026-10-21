package com.pixelquest.app.darood.ui.create

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Creating a party through its screen, at the ends of the member limit: 2 and 30 are made, and 1
 * and 31 can't be asked for, nor would the server take them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class CreatePartyLimitsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val db = androidx.room.Room.inMemoryDatabaseBuilder(
        androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>(),
        com.pixelquest.app.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "me").apply { usernames["me"] = "aria_k" }
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })

    @After
    fun closeDb() {
        db.close()
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(100) {
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("still waiting")
    }

    /** The create screen, as the player sees it in [theme]; the id of the party it opens, once made. */
    private fun openCreate(theme: ThemeMode): () -> String? {
        var opened: String? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = theme) {
                CreatePartyScreen(onNavigateBack = {}, onCreated = { id, _ -> opened = id }, viewModel = CreatePartyViewModel(repository))
            }
        }
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.CREATE_NAME))).performTextInput("Friday circle")
        return { opened }
    }

    private fun limit() = composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_LIMIT)

    private fun fewer() = composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_FEWER)

    private fun more() = composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_MORE)

    @Test
    fun aPartyOfTwo_isTheSmallest_andIsMade() {
        val opened = openCreate(ThemeMode.Pixel)
        repeat(8) { fewer().performClick() }
        limit().assertTextEquals("2")
        fewer().assertIsNotEnabled()

        // One more tap asks for 1: nothing changes.
        fewer().performClick()
        limit().assertTextEquals("2")
        more().assertIsEnabled()

        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_BUTTON).performClick()
        waitFor { opened() != null }
        assertEquals(2, server.parties.getValue(opened()!!).maxMembers)
    }

    @Test
    fun aPartyOf30_isTheLargest_andIsMade() {
        val opened = openCreate(ThemeMode.Comic)
        repeat(20) { more().performClick() }
        limit().assertTextEquals("30")
        more().assertIsNotEnabled()

        // One more tap asks for 31: nothing changes.
        more().performClick()
        limit().assertTextEquals("30")
        fewer().assertIsEnabled()

        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_BUTTON).performClick()
        waitFor { opened() != null }
        assertEquals(30, server.parties.getValue(opened()!!).maxMembers)
    }

    @Test
    fun theLimitReadsTheSame_inLight() {
        openCreate(ThemeMode.Comic)
        limit().assertTextEquals("10")
        more().performClick()
        limit().assertTextEquals("11")
        fewer().performClick()
        fewer().performClick()
        limit().assertTextEquals("9")
    }

    @Test
    fun oneAnd31_wouldBeRefused_evenIfAskedForAroundTheScreen() = runBlocking {
        for (count in listOf(1, 31)) {
            val refused = repository.createParty("Friday circle", count, null)
            assertTrue("$count: ${refused::class.simpleName}", refused is SupabaseResult.ServerError)
            assertEquals(DaroodRefusal.INVALID, DaroodRefusal.of(refused))
        }
        assertTrue("nothing made", server.parties.isEmpty())
        for (count in listOf(2, 30)) {
            assertTrue("$count", repository.createParty("Friday circle", count, null) is SupabaseResult.Success)
        }
    }
}
