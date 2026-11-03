package com.pixelquest.app.ui.screens.onboarding

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.domain.HeroName
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.ThemedTestContent
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase B - V021: Onboarding user flow and exhaustive out-of-bounds cases.
 *
 * Covers:
 * 1. Happy-path user journey: Welcome -> Name Entry -> Avatar Pick -> Difficulty Pick -> Summary -> Finish.
 * 2. Input boundaries: Empty, spaces only, 1 char, 20 chars limit, 21 chars, 10,000 chars.
 * 3. Hostile/Odd characters: Emoji, Arabic/RTL, Arabic-Indic digits, control chars, null bytes, SQL/HTML snippets.
 * 4. Step navigation & lifecycle: Skipping invalid steps, backing out halfway, state preservation.
 * 5. Theme and display configurations: Pixel, Comic, Dark, Light, Simple Mode.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class OnboardingFlowAndEdgeCasesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var difficultySettingsRepository: FakeDifficultySettingsRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        userProfileRepository = FakeUserProfileRepository()
        difficultySettingsRepository = FakeDifficultySettingsRepository()
        settingsRepository = FakeSettingsRepository(onboardingDone = false)
        viewModel = OnboardingViewModel(
            userProfileRepository = userProfileRepository,
            difficultySettingsRepository = difficultySettingsRepository,
            settingsRepository = settingsRepository,
        )
    }

    // -------------------------------------------------------------------------
    // 1. Full User Flow (UI & Navigation)
    // -------------------------------------------------------------------------

    @Test
    fun fullOnboardingJourney_simulatesCompletePath_andLandsOnHome() {
        var finished = false
        composeTestRule.setContent {
            ThemedTestContent(ComposeTestConfiguration(themeMode = ThemeMode.Pixel)) {
                OnboardingFlowScreen(
                    viewModel = viewModel,
                    onOnboardingComplete = { finished = true },
                )
            }
        }

        // 1. Welcome Screen
        composeTestRule.onNodeWithText("WELCOME, HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("START YOUR JOURNEY", ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // 2. Name Entry Screen
        composeTestRule.onNodeWithText("NAME YOUR HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Step 1 of 4").assertIsDisplayed()

        // Next button is disabled initially when name is empty
        composeTestRule.onNodeWithText("NEXT", ignoreCase = true).assertIsNotEnabled()

        // Type hero name
        composeTestRule.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("ArthurPendragon")
        composeTestRule.onNodeWithText("NEXT", ignoreCase = true).assertIsEnabled().performClick()

        // 3. Avatar Step Screen
        composeTestRule.onNodeWithText("CHOOSE YOUR AVATAR").assertIsDisplayed()
        composeTestRule.onNodeWithText("Step 2 of 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("NEXT", ignoreCase = true).performClick()

        // 4. Difficulty Step Screen
        composeTestRule.onNodeWithText("CHOOSE DIFFICULTY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Step 3 of 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("NEXT", ignoreCase = true).performClick()

        // 5. Summary Screen
        composeTestRule.onNodeWithText("YOUR HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Step 4 of 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("ARTHURPENDRAGON").assertExists()
        composeTestRule.onNodeWithText("BEGIN QUEST", ignoreCase = true)
            .assertHasClickAction()
            .performClick()

        // Verification of completion and persistence
        assertTrue("Onboarding complete callback must fire", finished)
        runBlocking {
            assertTrue("Settings must record onboarding done", settingsRepository.onboardingComplete.first())
            val profile = userProfileRepository.getProfile().first()
            assertNotNull("Profile must be persisted in database", profile)
            assertEquals("ArthurPendragon", profile?.username)
        }
    }

    // -------------------------------------------------------------------------
    // 2. Input Boundaries: Empty, 1 char, limit, limit+1, huge input
    // -------------------------------------------------------------------------

    @Test
    fun nameInput_emptyAndSpacesOnly_rejectedWithoutCrashing() {
        // Initial empty state: no error shown yet before typing
        viewModel.updateUsername("")
        assertFalse(viewModel.uiState.value.isNameValid)
        assertNull(viewModel.uiState.value.nameError)

        // Spaces only
        viewModel.updateUsername("    ")
        assertFalse(viewModel.uiState.value.isNameValid)
        assertEquals("Name cannot be blank", viewModel.uiState.value.nameError)

        // Attempting to advance must stay on NameEntry
        viewModel.goToStep(OnboardingStep.NameEntry)
        viewModel.nextStep()
        assertEquals(OnboardingStep.NameEntry, viewModel.uiState.value.currentStep)
    }

    @Test
    fun nameInput_minimumOneCharacter_isAccepted() {
        viewModel.updateUsername("X")
        assertTrue(viewModel.uiState.value.isNameValid)
        assertNull(viewModel.uiState.value.nameError)
        assertEquals("X", viewModel.uiState.value.username)

        viewModel.goToStep(OnboardingStep.NameEntry)
        viewModel.nextStep()
        assertEquals(OnboardingStep.AvatarPick, viewModel.uiState.value.currentStep)
    }

    @Test
    fun nameInput_exactLimit20Chars_isAccepted() {
        val exactName = "A".repeat(HeroName.MAX_LENGTH)
        viewModel.updateUsername(exactName)
        assertTrue(viewModel.uiState.value.isNameValid)
        assertNull(viewModel.uiState.value.nameError)
        assertEquals(20, viewModel.uiState.value.username.length)
    }

    @Test
    fun nameInput_beyondLimitAndHuge10000Chars_isClampedTo20Chars() {
        // Limit + 1 (21 characters)
        val name21 = "A".repeat(21)
        viewModel.updateUsername(name21)
        assertEquals(20, viewModel.uiState.value.username.length)
        assertTrue(viewModel.uiState.value.isNameValid)

        // 10,000 characters
        val hugeName = "Z".repeat(10_000)
        viewModel.updateUsername(hugeName)
        assertEquals(20, viewModel.uiState.value.username.length)
        assertEquals("Z".repeat(20), viewModel.uiState.value.username)
        assertTrue(viewModel.uiState.value.isNameValid)
        assertNull(viewModel.uiState.value.nameError)
    }

    // -------------------------------------------------------------------------
    // 3. Odd / Hostile Characters
    // -------------------------------------------------------------------------

    @Test
    fun nameInput_emojiText_isHandledProperly() {
        viewModel.updateUsername("Knight⚔️")
        assertTrue(viewModel.uiState.value.isNameValid)
        assertTrue(viewModel.uiState.value.username.startsWith("Knight"))
    }

    @Test
    fun nameInput_arabicAndRtlCharacters_isAccepted() {
        viewModel.updateUsername("بطل_الفرسان")
        assertTrue(viewModel.uiState.value.isNameValid)
        assertEquals("بطل_الفرسان", viewModel.uiState.value.username)
    }

    @Test
    fun nameInput_arabicIndicDigits_isAccepted() {
        viewModel.updateUsername("فارس١٢٣")
        assertTrue(viewModel.uiState.value.isNameValid)
        assertEquals("فارس١٢٣", viewModel.uiState.value.username)
    }

    @Test
    fun nameInput_controlCharactersAndNullBytes_areSanitized() {
        viewModel.updateUsername("Hero\u0000\u0007\u001BQuest")
        // Control characters and null bytes are stripped by InputRules.typed
        assertEquals("HeroQuest", viewModel.uiState.value.username)
        assertTrue(viewModel.uiState.value.isNameValid)
    }

    @Test
    fun nameInput_sqlAndHtmlInjectionSnippets_treatedAsPlainText() {
        val sqlSnippet = "'; DROP TABLE users;"
        viewModel.updateUsername(sqlSnippet)
        // Treated strictly as literal text and clamped to 20 chars
        assertEquals(sqlSnippet.take(20), viewModel.uiState.value.username)
        assertTrue(viewModel.uiState.value.isNameValid)

        val htmlSnippet = "<script>alert(1)</script>"
        viewModel.updateUsername(htmlSnippet)
        assertEquals(htmlSnippet.take(20), viewModel.uiState.value.username)
    }

    // -------------------------------------------------------------------------
    // 4. Navigation, Back Stack & Lifecycle
    // -------------------------------------------------------------------------

    @Test
    fun backOutHalfway_preservesAllSelectedOptions() {
        viewModel.goToStep(OnboardingStep.Welcome)
        viewModel.nextStep() // NameEntry

        viewModel.updateUsername("Paladin")
        viewModel.nextStep() // AvatarPick

        viewModel.updateAvatar("avatar_mage")
        viewModel.nextStep() // DifficultyPick

        viewModel.updateDifficulty(DifficultyLevel.HARD)

        // Step back all the way to Welcome
        viewModel.previousStep() // AvatarPick
        assertEquals(OnboardingStep.AvatarPick, viewModel.uiState.value.currentStep)
        viewModel.previousStep() // NameEntry
        assertEquals(OnboardingStep.NameEntry, viewModel.uiState.value.currentStep)
        viewModel.previousStep() // Welcome
        assertEquals(OnboardingStep.Welcome, viewModel.uiState.value.currentStep)

        // State remains intact
        assertEquals("Paladin", viewModel.uiState.value.username)
        assertEquals("avatar_mage", viewModel.uiState.value.avatarId)
        assertEquals(DifficultyLevel.HARD, viewModel.uiState.value.difficultyLevel)

        // Step forward again
        viewModel.nextStep() // NameEntry
        viewModel.nextStep() // AvatarPick
        viewModel.nextStep() // DifficultyPick
        viewModel.nextStep() // Summary
        assertEquals(OnboardingStep.Summary, viewModel.uiState.value.currentStep)
    }

    @Test
    fun completeOnboarding_multipleInvocations_isIdempotent() = runTest {
        viewModel.updateUsername("IdempotentHero")
        var fireCount = 0

        viewModel.completeOnboarding { fireCount++ }
        viewModel.completeOnboarding { fireCount++ }

        assertEquals(2, fireCount)
        val profile = userProfileRepository.getProfile().first()
        assertNotNull(profile)
        assertEquals(1L, profile?.id)
        assertEquals("IdempotentHero", profile?.username)
    }

    // -------------------------------------------------------------------------
    // 5. Theme Matrix: Comic, Pixel, Dark, Simple Mode
    // -------------------------------------------------------------------------

    @Test
    fun onboardingSummary_rendersInComicThemeAndSimpleModeWithoutCrashing() {
        viewModel.updateUsername("ComicHero")
        composeTestRule.setContent {
            ThemedTestContent(ComposeTestConfiguration(themeMode = ThemeMode.Comic, isDark = true, isSimpleMode = true)) {
                OnboardingSummaryScreen(
                    username = "ComicHero",
                    avatarId = "avatar_hero",
                    difficultyLevel = DifficultyLevel.EASY,
                    onConfirmClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("COMICHERO").assertExists()
        composeTestRule.onNodeWithText("BEGIN QUEST", ignoreCase = true).assertExists()
    }

    private fun runTest(block: suspend () -> Unit) = kotlinx.coroutines.test.runTest { block() }
}
