package com.pixelquest.app.darood.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Darood Party's flag is on by default, and the settings keep it once changed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodFeatureFlagTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun onByDefault_andKeptOnceTurnedOff() = runBlocking {
        assertTrue(SettingsRepositoryImpl(context).daroodAvailable.first())

        SettingsRepositoryImpl(context).setDaroodAvailable(false)

        assertEquals(false, SettingsRepositoryImpl(context).daroodAvailable.first())
    }
}
