package com.pixelquest.app.study.timer

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.study.data.MemoryStudyTimerStore
import com.pixelquest.app.study.data.PrefsStudyTimerStore
import com.pixelquest.app.study.data.RunningTimer
import com.pixelquest.app.study.data.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyTimerStateMachineTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun timerStateMachine_transitionsAccurately() {
        val store = MemoryStudyTimerStore()
        assertNull(store.running.value)

        // 1. Start timer at t = 1000s
        val t0 = 1_000_000L
        store.start("party_1", t0, "Physics Circle")
        val running = store.running.value
        assertNotNull(running)
        assertEquals(TimerState.RUNNING, running?.state)
        assertFalse(running?.isPaused == true)
        assertEquals(0L, running?.elapsedMillisAt(t0))
        assertEquals(30_000L, running?.elapsedMillisAt(t0 + 30_000L))

        // 2. Pause timer 10 minutes later (t = t0 + 600s)
        val t1 = t0 + 600_000L
        store.pause(t1)
        val paused = store.running.value
        assertNotNull(paused)
        assertEquals(TimerState.PAUSED, paused?.state)
        assertTrue(paused?.isPaused == true)
        // Elapsed time must be frozen at 600_000L regardless of current time
        assertEquals(600_000L, paused?.elapsedMillisAt(t1))
        assertEquals(600_000L, paused?.elapsedMillisAt(t1 + 300_000L))

        // 3. Resume timer 5 minutes later (t = t1 + 300s)
        val t2 = t1 + 300_000L
        store.resume(t2)
        val resumed = store.running.value
        assertNotNull(resumed)
        assertEquals(TimerState.RUNNING, resumed?.state)
        assertFalse(resumed?.isPaused == true)
        assertEquals(300_000L, resumed?.accumulatedPauseMs)
        // At t2, elapsed active time is still 600_000L
        assertEquals(600_000L, resumed?.elapsedMillisAt(t2))
        // 10 minutes after resuming (t2 + 600_000L), active time is 1,200_000L (20 mins)
        assertEquals(1_200_000L, resumed?.elapsedMillisAt(t2 + 600_000L))

        // 4. Stop and clear
        store.clear()
        assertNull(store.running.value)
    }

    @Test
    fun loggedAt_enforcesSessionRules() {
        val t0 = 1_000_000L
        val timer = RunningTimer("party_1", t0, "Math Circle")

        // Under 5 minutes: null (discarded)
        assertNull(timer.loggedAt(t0 + 4 * 60_000L))
        assertNull(timer.loggedAt(t0 + 4 * 60_000L + 59_000L))

        // 5 minutes: logged as 5
        assertEquals(5, timer.loggedAt(t0 + 5 * 60_000L))

        // 45 minutes: logged as 45
        assertEquals(45, timer.loggedAt(t0 + 45 * 60_000L))

        // 240 minutes (4 hours): logged as 240
        assertEquals(240, timer.loggedAt(t0 + 240 * 60_000L))

        // Over 4 hours: capped at 240
        assertEquals(240, timer.loggedAt(t0 + 300 * 60_000L))
    }

    @Test
    fun prefsStore_persistsAcrossProcessDeath() {
        val t0 = 2_000_000L
        val store1 = PrefsStudyTimerStore(context)
        store1.start("party_chem", t0, "Chemistry Circle")
        store1.pause(t0 + 120_000L) // paused after 2 mins

        // Simulate app restart / new process instantiating store from disk
        val store2 = PrefsStudyTimerStore(context)
        val restored = store2.running.value
        assertNotNull(restored)
        assertEquals("party_chem", restored?.partyId)
        assertEquals("Chemistry Circle", restored?.partyName)
        assertEquals(t0, restored?.startedAt)
        assertEquals(t0 + 120_000L, restored?.pausedAt)
        assertTrue(restored?.isPaused == true)
        assertEquals(120_000L, restored?.elapsedMillisAt(t0 + 500_000L))

        // Resume in restored instance
        store2.resume(t0 + 200_000L) // 80s of pause
        assertEquals(80_000L, store2.running.value?.accumulatedPauseMs)
        assertFalse(store2.running.value?.isPaused == true)

        // Clear in store2
        store2.clear()

        // Verify cleared in store3
        val store3 = PrefsStudyTimerStore(context)
        assertNull(store3.running.value)
    }

    @Test
    fun multiplePauseResumeCycles_accuratelyAccountsPauseDurations() {
        val store = MemoryStudyTimerStore()
        val t0 = 1_000_000L
        store.start("party_1", t0, "Physics Circle")

        // 1. Run 10 mins (600s), pause for 5 mins (300s)
        store.pause(t0 + 600_000L)
        store.resume(t0 + 900_000L)
        assertEquals(300_000L, store.running.value?.accumulatedPauseMs)
        assertEquals(600_000L, store.running.value?.elapsedMillisAt(t0 + 900_000L))

        // 2. Run another 15 mins (900s), pause for 10 mins (600s)
        val pause2 = t0 + 900_000L + 900_000L // t0 + 1800s
        store.pause(pause2)
        val resume2 = pause2 + 600_000L // t0 + 2400s
        store.resume(resume2)
        assertEquals(900_000L, store.running.value?.accumulatedPauseMs) // 300s + 600s = 900s
        // Active elapsed: 10m + 15m = 25m (1_500_000ms)
        assertEquals(1_500_000L, store.running.value?.elapsedMillisAt(resume2))

        // 3. Run another 10 mins (600s), pause at t0 + 3000s
        val pause3 = resume2 + 600_000L // t0 + 3000s
        store.pause(pause3)
        // Active elapsed: 25m + 10m = 35m (2_100_000ms)
        assertEquals(2_100_000L, store.running.value?.elapsedMillisAt(pause3))
        // Remains 35m even 2 hours later because it's paused
        assertEquals(2_100_000L, store.running.value?.elapsedMillisAt(pause3 + 7_200_000L))

        // 4. Chronometer base verification
        val runningAgain = store.running.value!!.copy(pausedAt = null)
        val realtimeBoot = 50_000_000L
        val base = runningAgain.chronometerBase(realtimeBoot, pause3)
        assertEquals(realtimeBoot - 2_100_000L, base)
    }

    @Test
    fun idempotentPauseAndResume_doesNotCorruptState() {
        val store = MemoryStudyTimerStore()
        val t0 = 1_000_000L

        // Resuming when null is a safe no-op
        store.resume(t0)
        assertNull(store.running.value)

        // Pausing when null is a safe no-op
        store.pause(t0)
        assertNull(store.running.value)

        store.start("party_1", t0, "Physics Circle")

        // Resuming when already running does not change state or accumulatedPauseMs
        store.resume(t0 + 10_000L)
        assertFalse(store.running.value?.isPaused == true)
        assertEquals(0L, store.running.value?.accumulatedPauseMs)

        // Pausing sets pausedAt
        store.pause(t0 + 30_000L)
        assertEquals(t0 + 30_000L, store.running.value?.pausedAt)

        // Pausing again while already paused preserves the initial pausedAt
        store.pause(t0 + 60_000L)
        assertEquals(t0 + 30_000L, store.running.value?.pausedAt)
        assertEquals(30_000L, store.running.value?.elapsedMillisAt(t0 + 100_000L))
    }

    @Test
    fun clockSkewOrNegativeElapsed_clampedToZero() {
        val t0 = 1_000_000L
        val timer = RunningTimer("party_1", t0, "Math Circle")

        // Time before start (e.g. clock adjusted backwards)
        assertEquals(0L, timer.elapsedMillisAt(t0 - 50_000L))
        assertEquals(0, timer.minutesAt(t0 - 50_000L))
        assertNull(timer.loggedAt(t0 - 50_000L))
    }
}
