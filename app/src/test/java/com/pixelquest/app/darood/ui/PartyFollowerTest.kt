package com.pixelquest.app.darood.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.testing.FakeDaroodLive
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A party is followed live only while its screen is up, and catches up when it comes back or is pulled. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PartyFollowerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val server = FakeDaroodServer(me = "me").apply { usernames["me"] = "aria_k" }
    private val live = FakeDaroodLive()
    private val repository = DaroodPartyRepositoryImpl(
        server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") }, live = live, numbersDelayMs = 0
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
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

    @Test
    fun followed_whileTheScreenIsStarted_andNotInTheBackground() {
        val friday = server.seedParty("Friday circle", "me")
        val follower = PartyFollower(repository, scope, friday)
        composeTestRule.setContent {
            FollowWhileStarted(follower)
            Text("Friday circle")
        }
        waitFor { live.following[friday] == 1 }
        assertEquals("it caught up first", "Friday circle", runBlocking { repository.observeParty(friday).first()?.name })

        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        waitFor { live.following[friday] == 0 }

        // Back on screen: it catches up with what was missed, and follows again.
        server.seedEntry(friday, "me", 313)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitFor { live.following[friday] == 1 }
        waitFor { runBlocking { repository.observeEntries(friday, 50).first().size } == 1 }
    }

    @Test
    fun pulling_refreshes_andSaysWhenTheServerCantBeReached() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        val follower = PartyFollower(repository, scope, friday)

        // Room runs its queries on its own threads, so each refresh is waited for.
        fun pull() {
            follower.refresh()
            waitFor { !follower.refreshing.value }
        }

        pull()
        assertFalse(follower.offline.value)
        assertEquals("Friday circle", repository.observeParty(friday).first()?.name)

        server.offline = true
        pull()
        assertTrue(follower.offline.value)

        // Back online, a pull sends what waited.
        repository.addEntry(friday, 33)
        server.offline = false
        pull()
        assertFalse(follower.offline.value)
        assertEquals(listOf(33), server.entries.map { it.amount })
    }
}
