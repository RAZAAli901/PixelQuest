package com.pixelquest.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoundSettingsPersistenceTest {

    @Test
    fun soundSetting_persistsAcrossAppRestart() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo1 = SettingsRepositoryImpl(context)

        repo1.setSoundEnabled(false)
        assertFalse(repo1.isSoundEnabled.first())

        // Simulate app restart by creating a new repository instance
        val repo2 = SettingsRepositoryImpl(context)
        assertFalse(repo2.isSoundEnabled.first())

        // Re-enable sound
        repo2.setSoundEnabled(true)
        assertTrue(repo2.isSoundEnabled.first())
    }
}
