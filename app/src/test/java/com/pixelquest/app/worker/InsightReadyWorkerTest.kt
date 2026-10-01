package com.pixelquest.app.worker

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.notification.NotificationChannels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class InsightReadyWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private class FakeSettings(ai: Boolean, notifications: Boolean, simple: Boolean = false) : SettingsRepository {
        override val aiInsightsEnabled: Flow<Boolean> = MutableStateFlow(ai)
        override val simpleModeEnabled: Flow<Boolean> = MutableStateFlow(simple)
        override val isNotificationsEnabled: Flow<Boolean> = MutableStateFlow(notifications)
        override val isSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isCrtEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val isHapticsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isReduceMotionEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val onboardingComplete: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setCrtEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}
    }

    private fun run(settings: SettingsRepository): ListenableWorker.Result = runBlocking {
        val worker = TestListenableWorkerBuilder<InsightReadyWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    InsightReadyWorker(appContext, workerParameters, settings)
            })
            .build()
        worker.doWork()
    }

    @Before
    fun setUp() {
        NotificationChannels.createAll(context)
        manager.cancelAll()
    }

    @Test
    fun postsOnCoachChannel_whenAiAndNotificationsOn() {
        assertEquals(ListenableWorker.Result.success(), run(FakeSettings(ai = true, notifications = true)))
        val posted = shadowOf(manager).allNotifications
        assertEquals(1, posted.size)
        assertEquals(NotificationChannels.Spec.COACH.id, posted.single().channelId)
    }

    @Test
    fun staysSilent_whenAiCoachOff() {
        run(FakeSettings(ai = false, notifications = true))
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun staysSilent_whenNotificationsOff() {
        run(FakeSettings(ai = true, notifications = false))
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun simpleModeCopy_hasNoGameLanguage() {
        val (title, text) = InsightReadyWorker.copy(isSimpleMode = true)
        val copy = (title + " " + text).replace("PixelQuest", "")
        listOf("quest", "intel", "hero").forEach {
            assertFalse("'$it' in: $copy", copy.contains(it, ignoreCase = true))
        }
    }
}
