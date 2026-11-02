package com.pixelquest.app.testing

import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.di.CoroutineDispatchers
import com.pixelquest.app.scheduling.AlarmScheduler
import com.pixelquest.app.util.AppClock
import com.pixelquest.app.util.NetworkMonitor
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TestHiltModuleTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var fakeClock: FakeClock

    @Inject
    lateinit var appClock: AppClock

    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var fakeNetworkMonitor: FakeNetworkMonitor

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var fakeAlarmScheduler: FakeAlarmScheduler

    @Inject
    lateinit var appDatabase: AppDatabase

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testHiltModule_injectsFakeDependencies() {
        assertNotNull(clock)
        assertTrue(clock is FakeClock)
        assertNotNull(fakeClock)

        assertNotNull(networkMonitor)
        assertTrue(networkMonitor is FakeNetworkMonitor)
        assertNotNull(fakeNetworkMonitor)

        assertNotNull(alarmScheduler)
        assertTrue(alarmScheduler is FakeAlarmScheduler)
        assertNotNull(fakeAlarmScheduler)

        assertNotNull(appDatabase)
        assertNotNull(appDatabase.taskDao())
        appDatabase.openHelper.writableDatabase
        assertTrue(appDatabase.isOpen)
    }
}
