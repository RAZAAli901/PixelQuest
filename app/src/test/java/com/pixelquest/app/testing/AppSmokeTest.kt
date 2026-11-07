package com.pixelquest.app.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pixelquest.app.MainActivity
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.screens.HomeTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HiltTestApplication::class)
class AppSmokeTest {

    val hiltRule = HiltAndroidRule(this)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeTestRule)

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var fakeClock: FakeClock

    @Inject
    lateinit var fakeNetworkMonitor: FakeNetworkMonitor

    @Inject
    lateinit var fakeAlarmScheduler: FakeAlarmScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            settingsRepository.setOnboardingComplete(true)
            DatabaseSeeder(appDatabase).seedNewUser()
        }
    }

    @Test
    fun appSmokeTest_launchesWithFakes_andReachesHomeScreen() {
        // Assert that the test harness successfully injected in-memory/fake singletons
        assertNotNull(appDatabase)
        assertNotNull(fakeClock)
        assertNotNull(fakeNetworkMonitor)
        assertNotNull(fakeAlarmScheduler)
        assertTrue(appDatabase.isOpen)
        assertTrue(fakeNetworkMonitor.isCurrentlyOnline())

        // The app launches through MainActivity, displays Splash and navigates to Home
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(HomeTags.SCREEN).assertIsDisplayed()
    }
}
