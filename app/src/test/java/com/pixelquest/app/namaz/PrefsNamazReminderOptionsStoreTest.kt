package com.pixelquest.app.namaz

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.namaz.NamazReminderOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The reminder options outside the database: off by default, and kept once changed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PrefsNamazReminderOptionsStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun theNudge_isOffUntilTurnedOn_andStaysOn() = runBlocking {
        val store = PrefsNamazReminderOptionsStore(context)
        assertEquals(NamazReminderOptions(), store.get())

        store.update { it.copy(nudge = true) }

        // A new store, as after the app restarts, reads the same file.
        val again = PrefsNamazReminderOptionsStore(context)
        assertTrue(again.get().nudge)
        assertTrue(again.options.first().nudge)
    }
}
