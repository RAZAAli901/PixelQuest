package com.pixelquest.app.auth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.prefs.EncouragementPack
import com.pixelquest.app.data.local.prefs.EncouragementPackStore
import com.pixelquest.app.domain.ai.HabitInsightTone
import com.pixelquest.app.testing.FakeAiAccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Signing out drops the AI-written reminder lines, so reminders go back to the built-in ones at once. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AiSignOutCleanupTest {

    private val store = EncouragementPackStore(ApplicationProvider.getApplicationContext<Context>())
    private val pack = EncouragementPack(LocalDate.now(), HabitInsightTone.GAMIFIED_HEROIC, listOf("Go, hero!"))

    @Test
    fun signingOut_clearsTheSavedLines_signedInKeepsThem() = runTest(UnconfinedTestDispatcher()) {
        val access = FakeAiAccess()
        store.save(pack)
        val job = AiSignOutCleanup.start(this, { access }, store)

        assertNotNull("Signed in: kept", store.load())

        access.token = null
        assertNull("Signed out: gone", store.load())

        job.cancel()
    }
}
