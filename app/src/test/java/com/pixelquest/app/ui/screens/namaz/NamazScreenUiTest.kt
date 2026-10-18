package com.pixelquest.app.ui.screens.namaz

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.namazViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The Namaz screen with its ViewModel on fakes, in Lahore at 16:00 on 17 October 2026 (after Asr,
 * before Maghrib), unless a test moves the clock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NamazScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
    private val clock = FixedClock(LocalDateTime.of(2026, 10, 17, 16, 0))
    private val settings = FakeSettingsRepository(onboardingDone = true)

    private val reminderOptions = com.pixelquest.app.testing.FakeNamazReminderOptionsStore()
    private val alarms = com.pixelquest.app.testing.FakeNamazAlarms()

    private fun show(mode: ThemeMode = ThemeMode.Pixel) {
        val viewModel = namazViewModel(repository, clock, settings = settings, reminderOptions = reminderOptions, alarms = alarms)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) { NamazScreen(viewModel = viewModel) }
        }
        composeTestRule.waitForIdle()
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun fiveSeparateButtons_inTheDaysOrder() {
        show()

        Prayer.entries.forEach { prayer ->
            val button = NamazTestTags.prayer(prayer)
            scrollTo(button)
            composeTestRule.onNodeWithTag(button).assertIsDisplayed()
                .assertTextContains(composeTestRule.activity.getString(prayer.displayName), substring = true)
        }
        // Each one below the one before it.
        Prayer.entries.zipWithNext().forEach { (earlier, later) ->
            scrollTo(NamazTestTags.prayer(later))
            assertTrue("$later comes after $earlier", top(earlier) < top(later))
        }
    }

    private fun top(prayer: Prayer) =
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(prayer)).fetchSemanticsNode().positionInRoot.y

    @Test
    fun aPrayerNotStartedYet_isLocked_andTappingItSaysWhenItStarts() {
        show()
        val maghrib = NamazTestTags.prayer(Prayer.MAGHRIB)
        scrollTo(maghrib)
        composeTestRule.onNodeWithTag(maghrib).assertTextContains("Not yet", substring = true)

        composeTestRule.onNodeWithTag(maghrib).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.LOCKED_TOOLTIP).assertIsDisplayed()
        composeTestRule.onNodeWithText("Starts at", substring = true).assertIsDisplayed()
        // Nothing was marked.
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun tappingAPrayerThatHasStarted_marksItPrayed() {
        show()
        val asr = NamazTestTags.prayer(Prayer.ASR)
        scrollTo(asr)
        composeTestRule.onNodeWithTag(asr).assertTextContains("Tap when prayed", substring = true)

        composeTestRule.onNodeWithTag(asr).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(asr).assertTextContains("Prayed", substring = true)
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.ASR))
    }

    @Test
    fun aMissedPrayer_canStillBeMarked_late() {
        show()
        val dhuhr = NamazTestTags.prayer(Prayer.DHUHR)
        scrollTo(dhuhr)
        composeTestRule.onNodeWithTag(dhuhr).assertTextContains("Missed", substring = true)

        composeTestRule.onNodeWithTag(dhuhr).performClick()
        composeTestRule.waitForIdle()

        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.DHUHR))
    }

    @Test
    fun tappingAPrayedPrayer_asksFirst_thenUndoTakesItBack() {
        repository.rows.value = emptyMap()
        kotlinx.coroutines.runBlocking { repository.logPrayer(day, Prayer.ASR, prayedAtMillis = 0) }
        show()
        val asr = NamazTestTags.prayer(Prayer.ASR)
        scrollTo(asr)

        composeTestRule.onNodeWithTag(asr).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mark Asr as not prayed?").assertIsDisplayed()
        // Only asked: still prayed.
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.ASR))

        composeTestRule.onNodeWithText("Undo").performClick()
        composeTestRule.waitForIdle()

        assertEquals(PrayerStatus.PENDING, repository.statusOf(day, Prayer.ASR))
        composeTestRule.onNodeWithTag(asr).assertTextContains("Tap when prayed", substring = true)
    }

    @Test
    fun aLongPressOnAPrayedPrayer_asksToTakeItBack_too() {
        kotlinx.coroutines.runBlocking { repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = 0) }
        show()
        val fajr = NamazTestTags.prayer(Prayer.FAJR)
        scrollTo(fajr)

        composeTestRule.onNodeWithTag(fajr).performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Mark Fajr as not prayed?").assertIsDisplayed()
    }

    @Test
    fun theProgress_countsThePrayersPrayed() {
        kotlinx.coroutines.runBlocking {
            repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = 0)
            repository.logPrayer(day, Prayer.DHUHR, prayedAtMillis = 0)
        }
        show()
        scrollTo(NamazTestTags.PROGRESS)
        composeTestRule.onNodeWithText("2 / 5 prayed today").assertIsDisplayed()

        scrollTo(NamazTestTags.prayer(Prayer.ASR))
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.ASR)).performClick()
        composeTestRule.waitForIdle()

        scrollTo(NamazTestTags.PROGRESS)
        composeTestRule.onNodeWithText("3 / 5 prayed today").assertIsDisplayed()
    }

    @Test
    fun anEarlierDay_fromTheStrip_isShownReadOnly() {
        val yesterday = day.minusDays(1)
        kotlinx.coroutines.runBlocking { repository.logPrayer(yesterday, Prayer.FAJR, prayedAtMillis = 0) }
        show()
        scrollTo(NamazTestTags.DAY_STRIP)

        composeTestRule.onNodeWithTag(NamazTestTags.day(yesterday)).performClick()
        composeTestRule.waitForIdle()

        // The heading sits just above the day's progress.
        scrollTo(NamazTestTags.PROGRESS)
        composeTestRule.onNodeWithText("Prayers on", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 5 prayed that day").assertIsDisplayed()
        // Tapping its prayers changes nothing.
        scrollTo(NamazTestTags.prayer(Prayer.ASR))
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.ASR)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(null, repository.statusOf(yesterday, Prayer.ASR))
        assertEquals(null, repository.statusOf(day, Prayer.ASR))
    }

    @Test
    fun theGear_opensTheSettings_andAMinuteLaterMovesTheTime() {
        show()
        composeTestRule.onNodeWithContentDescription("Namaz settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NamazTestTags.SETTINGS).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Fajr a minute later").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, repository.settings.value.adjustFajr)
        composeTestRule.onNodeWithText("+1 min").assertExists()
    }

    @Test
    fun whileTheTrackerIsOff_thereAreNoSettings_andTheExplainerShows() {
        repository.settings.value = NamazSettingsEntity(enabled = false)
        show()

        composeTestRule.onNodeWithTag(NamazTestTags.EXPLAINER).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Namaz settings").assertDoesNotExist()
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.FAJR)).assertDoesNotExist()
    }

    @Test
    fun pickingACity_movesTheTimesThere() {
        show()
        scrollTo(NamazTestTags.LOCATION)
        composeTestRule.onNodeWithText("Times for Lahore").assertIsDisplayed()

        composeTestRule.onNodeWithText("Pick a city", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NamazTestTags.CITY_PICKER).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("kara")
        composeTestRule.onNodeWithText("Karachi").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Karachi", repository.settings.value.city)
        scrollTo(NamazTestTags.LOCATION)
        composeTestRule.onNodeWithText("Times for Karachi").assertIsDisplayed()
    }

    @Test
    fun theHijriDate_isUnderTheTitle() {
        show()
        composeTestRule.onNodeWithTag(NamazTestTags.HIJRI).assertIsDisplayed()
            .assertTextContains("Jumada al-Ula 1448 AH", substring = true)
    }

    @Test
    fun theStreak_hasItsFlame_butNotInSimpleMode() {
        kotlinx.coroutines.runBlocking {
            Prayer.entries.forEach { repository.logPrayer(day.minusDays(1), it, prayedAtMillis = 0) }
        }
        settings.simpleModeEnabled.value = true
        show()
        scrollTo(NamazTestTags.STREAK)

        composeTestRule.onNodeWithTag(NamazTestTags.STREAK).assertTextContains("1 day in a row with all five prayed")
    }

    @Test
    fun theStreak_hasItsFlame_inTheGameThemes() {
        kotlinx.coroutines.runBlocking {
            Prayer.entries.forEach { repository.logPrayer(day.minusDays(1), it, prayedAtMillis = 0) }
        }
        show(ThemeMode.Comic)
        scrollTo(NamazTestTags.STREAK)

        composeTestRule.onNodeWithTag(NamazTestTags.STREAK).assertTextContains("🔥 1 day in a row with all five prayed")
    }

    private fun clickLabel(tag: String): String? =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().config
            .getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick)?.label

    @Test
    fun talkBack_readsEachPrayer_andWhatADoubleTapDoes() {
        kotlinx.coroutines.runBlocking { repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = 0) }
        show()

        val expected = mapOf(
            Prayer.FAJR to ("prayed" to "mark not prayed"),
            Prayer.DHUHR to ("missed" to "mark prayed late"),
            Prayer.ASR to ("not prayed" to "mark prayed"),
            Prayer.MAGHRIB to ("starts at" to "hear when it starts")
        )
        expected.forEach { (prayer, said) ->
            val tag = NamazTestTags.prayer(prayer)
            scrollTo(tag)
            val name = composeTestRule.activity.getString(prayer.displayName)
            composeTestRule.onNodeWithTag(tag).assert(
                androidx.compose.ui.test.hasContentDescription("$name, ", substring = true)
                    .and(androidx.compose.ui.test.hasContentDescription(", ${said.first}", substring = true))
            )
            assertEquals(said.second, clickLabel(tag))
        }
        // "Asr, 3:50 PM, not prayed" in full.
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.ASR))
            .assert(androidx.compose.ui.test.hasContentDescription("Asr, 3:50", substring = true))
    }

    @Test
    fun talkBack_offersNothingToDo_onAnEarlierDay() {
        show()
        scrollTo(NamazTestTags.DAY_STRIP)
        composeTestRule.onNodeWithTag(NamazTestTags.day(day.minusDays(2))).performClick()
        composeTestRule.waitForIdle()

        scrollTo(NamazTestTags.prayer(Prayer.ASR))
        assertEquals(null, clickLabel(NamazTestTags.prayer(Prayer.ASR)))
    }

    @Test
    fun theSettings_turnOnePrayersReminderOff() {
        show()
        composeTestRule.onNodeWithContentDescription("Namaz settings").performClick()
        composeTestRule.waitForIdle()

        // The Reminders section lists each prayer by name with a switch.
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Isha").and(androidx.compose.ui.test.isToggleable()))
            .onFirst().performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(false, repository.settings.value.remindIsha)
        assertEquals(true, repository.settings.value.remindFajr)
    }

    @Test
    fun theSettings_pickTheReminderSound() {
        show()
        composeTestRule.onNodeWithContentDescription("Namaz settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("PixelQuest chime").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(com.pixelquest.app.domain.namaz.NamazSound.CHIME, reminderOptions.options.value.sound)

        composeTestRule.onNodeWithText("Silent (no sound or vibration)").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(com.pixelquest.app.domain.namaz.NamazSound.SILENT, reminderOptions.options.value.sound)
    }

    @Test
    fun theDebugTestButton_armsTheCurrentPrayersReminder_in10Seconds() {
        show()
        composeTestRule.onNodeWithContentDescription("Namaz settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.TEST_REMINDER).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val test = alarms.armed.values.single()
        assertEquals(com.pixelquest.app.domain.namaz.NamazAlarm.Kind.TEST, test.kind)
        // 16:00 is in Asr's time.
        assertEquals(Prayer.ASR, test.prayer)
        assertEquals(clock.time.plusSeconds(10).atZone(com.pixelquest.app.testing.LahoreZone).toInstant().toEpochMilli(), test.atMillis)
    }

    @Test
    fun theSettings_turnNamazXpOff() {
        show()
        composeTestRule.onNodeWithContentDescription("Namaz settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Earn XP for prayers").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(false, repository.settings.value.xpEnabled)
    }

    @Test
    fun simpleMode_hasNoXpInTheSettings() {
        settings.simpleModeEnabled.value = true
        show()
        composeTestRule.onNodeWithContentDescription("Namaz settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Earn XP for prayers").assertDoesNotExist()
        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("XP", substring = true)).fetchSemanticsNodes().size)
    }

    @Test
    fun simpleMode_stillMarksPrayers() {
        settings.simpleModeEnabled.value = true
        show()

        scrollTo(NamazTestTags.prayer(Prayer.ASR))
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.ASR)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.ASR))
    }

    @Test
    fun markingAPrayer_popsUpItsXp_thenItGoes() {
        show()
        scrollTo(NamazTestTags.prayer(Prayer.ASR))
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.ASR)).performClick()
        // Then paused, to catch the popup before it fades (it's gone within a second): the mark
        // runs on the main looper, then a few frames of the popup.
        composeTestRule.mainClock.autoAdvance = false
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        composeTestRule.mainClock.advanceTimeBy(200)

        composeTestRule.onNodeWithTag(NamazTestTags.XP_POPUP, useUnmergedTree = true).assertExists()
            .assertTextContains("+10 XP")

        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.onNodeWithTag(NamazTestTags.XP_POPUP, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun inSimpleMode_markingShowsNoXpPopup() {
        settings.simpleModeEnabled.value = true
        show()
        scrollTo(NamazTestTags.prayer(Prayer.ASR))
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.ASR)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.XP_POPUP, useUnmergedTree = true).assertDoesNotExist()
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.ASR))
    }
}
