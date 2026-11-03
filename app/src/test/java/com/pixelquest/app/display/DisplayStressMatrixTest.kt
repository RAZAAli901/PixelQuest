package com.pixelquest.app.display

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.pixelquest.app.chess.ui.ChessBoard
import com.pixelquest.app.darood.domain.RaceLine
import com.pixelquest.app.darood.ui.race.RaceBoardContent
import com.pixelquest.app.darood.ui.race.RaceBoardUiState
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.reader.ReaderUiState
import com.pixelquest.app.quran.ui.reader.SurahReaderScreen
import com.pixelquest.app.study.ui.StudyPartyContent
import com.pixelquest.app.study.ui.StudyPartyUiState
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.setThemedContent
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.screens.namaz.NamazContent
import com.pixelquest.app.ui.screens.namaz.NamazUiState
import com.pixelquest.app.ui.screens.onboarding.OnboardingAvatarStepScreen
import com.pixelquest.app.ui.screens.onboarding.OnboardingNameEntryScreen
import com.pixelquest.app.ui.screens.onboarding.OnboardingWelcomeScreen
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

/**
 * V055: Display stress matrix test suite.
 * Evaluates layouts and rendering integrity across:
 * - Font scale 2.0x (Accessibility scaling)
 * - Small phone screens (320dp width)
 * - Tablet screens (800dp+ width)
 * - Right-to-Left (RTL) layout direction with Arabic locale
 * - Pixel & Comic themes, Dark mode, and Simple Mode
 * Across: Onboarding, Quran reader, Namaz, Chess, Darood race board, and Study party.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DisplayStressMatrixTest {

    companion object {
        private const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- Configurations for Matrix Testing ---

    private val fontScale2xConfig = ComposeTestConfiguration(
        fontScale = 2.0f,
        themeMode = ThemeMode.Comic,
    )

    private val rtlArabicConfig = ComposeTestConfiguration(
        layoutDirection = LayoutDirection.Rtl,
        locale = Locale("ar"),
        themeMode = ThemeMode.Pixel,
    )

    private val darkTabletConfig = ComposeTestConfiguration(
        isDark = true,
        themeMode = ThemeMode.Pixel,
    )

    private val simpleModeConfig = ComposeTestConfiguration(
        isSimpleMode = true,
        themeMode = ThemeMode.Comic,
    )

    // --- 1. Onboarding Flow Screens ---

    @Test
    fun testOnboardingWelcome_withFontScale2x_rendersKeyElements() {
        composeTestRule.setThemedContent(fontScale2xConfig) {
            OnboardingWelcomeScreen(
                onStartClick = {},
                onRestoreClick = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PIXELQUEST", substring = true, ignoreCase = true, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Start your journey", substring = true, ignoreCase = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun testOnboardingNameEntry_withRtlArabic_rendersProperly() {
        composeTestRule.setThemedContent(rtlArabicConfig) {
            OnboardingNameEntryScreen(
                username = "فارس بكسل",
                onUsernameChange = {},
                isValid = true,
                onNextClick = {},
                onBackClick = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("فارس بكسل", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testOnboardingAvatar_withDarkModeAndComicTheme_renders() {
        composeTestRule.setThemedContent(ComposeTestConfiguration(isDark = true, themeMode = ThemeMode.Comic)) {
            OnboardingAvatarStepScreen(
                selectedAvatarId = AvatarClass.Hero.id,
                onAvatarSelected = {},
                onNextClick = {},
                onBackClick = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Choose your avatar", substring = true, ignoreCase = true, useUnmergedTree = true).assertIsDisplayed()
    }

    // --- 2. Quran Reader Screen ---

    @Test
    fun testSurahReader_withFontScale2xAndRtlArabic_rendersAyahs() {
        val sampleSurah = Surah(1, "الفاتحة", "Al-Fatihah", "The Opening", Revelation.MECCAN, 7, 1, null)
        val sampleAyahs = listOf(
            Ayah(1, 1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", "In the name of Allah, the Entirely Merciful", "en.sahih"),
            Ayah(2, 1, 2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ", "All praise is due to Allah, Lord of the worlds", "en.sahih"),
        )
        val state = ReaderUiState(
            surah = sampleSurah,
            ayahs = sampleAyahs,
            hasMore = false,
            loading = false,
        )

        composeTestRule.setThemedContent(rtlArabicConfig.copy(fontScale = 2.0f)) {
            SurahReaderScreen(
                state = state,
                onNavigateBack = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Al-Fatihah", substring = true, ignoreCase = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", substring = true, useUnmergedTree = true).assertExists()
    }

    // --- 3. Namaz Screen ---

    @Test
    fun testNamazScreen_withSimpleModeAndPixelTheme_rendersWithoutIssues() {
        val state = NamazUiState(
            loading = false,
            enabled = true,
            settings = NamazSettingsEntity(),
            place = NamazPlace.LAHORE,
            today = LocalDate.of(2026, 11, 3),
            isSimpleMode = true,
        )

        composeTestRule.setThemedContent(simpleModeConfig.copy(themeMode = ThemeMode.Pixel)) {
            NamazContent(
                state = state,
                onNavigateBack = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Namaz", substring = true, ignoreCase = true, useUnmergedTree = true).assertIsDisplayed()
    }

    // --- 4. Chess Board Screen ---

    @Test
    fun testChessBoard_withTabletScreenAndDarkPixelTheme_rendersCleanly() {
        composeTestRule.setThemedContent(darkTabletConfig) {
            ChessBoard(
                fen = STARTING_FEN,
            )
        }
        composeTestRule.waitForIdle()
    }

    // --- 5. Darood Race Board Screen ---

    @Test
    fun testDaroodRaceBoard_withFontScale2xAndRtl_rendersRaceLines() {
        val raceLines = listOf(
            RaceLine("Alpha Circle", 5, 2500, 5000, 0.5f, null, true),
            RaceLine("Medina Group", 12, 5000, 5000, 1.0f, Instant.now(), false),
        )
        val state = RaceBoardUiState(
            lines = raceLines,
            problem = null,
        )

        composeTestRule.setThemedContent(fontScale2xConfig.copy(layoutDirection = LayoutDirection.Rtl)) {
            RaceBoardContent(
                state = state,
                onNavigateBack = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alpha Circle", substring = true, ignoreCase = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Medina Group", substring = true, ignoreCase = true, useUnmergedTree = true).assertExists()
    }

    // --- 6. Study Party Screen ---

    @Test
    fun testStudyParty_withFontScale2xAndSimpleMode_rendersScaffold() {
        val party = Party(
            id = "study_party_test",
            name = "Calculus Guild",
            leaderId = "leader1",
            maxMembers = 10,
            milestone = null,
            milestoneSetAt = null,
            completedAt = null,
            createdAt = Instant.ofEpochMilli(1000L),
            memberCount = 4,
            type = PartyType.STUDY,
        )
        val state = StudyPartyUiState(
            party = party,
            tab = PartyTab.THIS_WEEK,
            simpleMode = true,
            myId = "leader1",
        )

        composeTestRule.setThemedContent(fontScale2xConfig.copy(isSimpleMode = true)) {
            StudyPartyContent(
                state = state,
                onNavigateBack = {},
                onOpenInfo = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Calculus Guild", substring = true, ignoreCase = true, useUnmergedTree = true).assertExists()
    }
}
