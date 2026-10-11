package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.PendingLeaderboardChoice
import com.pixelquest.app.data.repository.PendingProgressReset
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Coming back online syncs whenever there's something to send. A phone off the leaderboard with a
 * leave still waiting used to get no sync, so its player stayed public until the next completed quest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackOnlineSyncTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun backOnline(
        linked: Boolean = true,
        onTheBoard: Boolean = false,
        leaveWaiting: Boolean = false,
        resetWaiting: Boolean = false
    ): Int = runBlocking {
        val tag = "$linked-$onTheBoard-$leaveWaiting-$resetWaiting"
        val choice = PendingLeaderboardChoice(context.getSharedPreferences("online-choice-$tag", Context.MODE_PRIVATE))
        val resets = PendingProgressReset(context.getSharedPreferences("online-reset-$tag", Context.MODE_PRIVATE))
        if (leaveWaiting) choice.choose("acct", optIn = false, displayName = null)
        if (resetWaiting) resets.mark("acct")
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Hero", avatarId = "avatar_hero", supabaseUserId = if (linked) "acct" else null, leaderboardOptIn = onTheBoard)
        )
        val scheduler = RecordingSyncScheduler()
        ConnectivitySyncObserver(context, scheduler, profiles, choice, resets).onBackOnline()
        scheduler.callCount
    }

    @Test
    fun onTheBoard_syncs() = assertEquals(1, backOnline(onTheBoard = true))

    @Test
    fun offTheBoard_withALeaveWaiting_syncs() = assertEquals(1, backOnline(leaveWaiting = true))

    @Test
    fun offTheBoard_withAResetWaiting_syncs() = assertEquals(1, backOnline(resetWaiting = true))

    @Test
    fun offTheBoard_withNothingToSend_doesNot() = assertEquals(0, backOnline())

    @Test
    fun notSignedIn_doesNot() = assertEquals(0, backOnline(linked = false, onTheBoard = true))
}
