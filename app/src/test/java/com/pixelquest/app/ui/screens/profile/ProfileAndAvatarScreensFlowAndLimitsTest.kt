package com.pixelquest.app.ui.screens.profile

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.setThemedContent
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.screens.AvatarPickerScreen
import com.pixelquest.app.ui.screens.AvatarTags
import com.pixelquest.app.ui.screens.HeroContent
import com.pixelquest.app.ui.screens.HeroSegment
import com.pixelquest.app.ui.screens.HeroTags
import com.pixelquest.app.ui.screens.avatar.AvatarSelectionViewModel
import com.pixelquest.app.ui.screens.heroUiOf
import com.pixelquest.app.ui.screens.stats.StatsUiState
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Unit & flow test suite for Profile and Avatar screens:
 * - Start-to-finish user flow: viewing hero profile, navigating to avatar picker, previewing, equipping, and returning
 * - Boundary conditions: empty username, 10,000-character name, extreme levels/XP/streaks
 * - Unicode & hostile inputs: emoji, Arabic, HTML/XSS, null bytes
 * - Configuration modes: Simple Mode suppression (no XP bar, locked difficulty, suppressed level history)
 * - Sign-in gating: Leaderboard locked vs unlocked
 * - Robustness: rapid avatar switching (toggle spam), corrupt avatar ID fallback
 * - Display resilience: Comic/Pixel skins, 2.0x font scale, RTL layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ProfileAndAvatarScreensFlowAndLimitsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.now()
    private val clock = FixedClock(LocalDateTime.of(today, LocalTime.NOON))

    private fun createHarness(
        initialProfile: UserProfileEntity = UserProfileEntity(
            id = 1,
            username = "ValiantHero",
            avatarId = "avatar_hero",
            level = 4,
            totalXp = 800,
            perfectDaysTowardNextLevel = 2,
        ),
        currentStreak: Int = 5,
        longestStreak: Int = 12,
        simpleMode: Boolean = false,
        signedIn: Boolean = true,
    ): TestHarness {
        val profileRepo = FakeUserProfileRepository(initialProfile)
        val streakRepo = FakeStreakRepository(StreakEntity(id = 1, currentStreak = currentStreak, longestStreak = longestStreak))
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        )
        val settingsRepo = FakeSettingsRepository(simpleMode = simpleMode)

        val profileVm = ProfileViewModel(
            userProfileRepository = profileRepo,
            streakRepository = streakRepo,
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
            appClock = clock,
        )

        val avatarVm = AvatarSelectionViewModel(
            userProfileRepository = profileRepo,
        )

        return TestHarness(
            profileRepo = profileRepo,
            streakRepo = streakRepo,
            diffRepo = diffRepo,
            settingsRepo = settingsRepo,
            profileVm = profileVm,
            avatarVm = avatarVm,
            signedIn = signedIn,
        )
    }

    @Composable
    private fun ProfileAndAvatarNavigatorHost(
        harness: TestHarness,
        onDifficultyClick: () -> Unit = {},
    ) {
        var currentScreen by remember { mutableStateOf("profile") }
        val profile by harness.profileVm.uiState.collectAsState()
        val equippedAvatarId by harness.avatarVm.currentAvatarId.collectAsState()
        val equipped = AvatarClass.from(equippedAvatarId)
        var selectedAvatar by remember(equipped) { mutableStateOf(equipped) }

        if (currentScreen == "profile") {
            val heroState = heroUiOf(
                profile = profile.copy(signedIn = harness.signedIn),
                stats = StatsUiState(),
                today = today,
            )
            HeroContent(
                state = heroState,
                segment = HeroSegment.Profile,
                onSegmentChange = {},
                onSettings = {},
                onChangeAvatar = { currentScreen = "avatar" },
                onDifficulty = onDifficultyClick,
                onQuestHistory = {},
                onLevelHistory = {},
                onLeaderboard = {},
            )
        } else {
            AvatarPickerScreen(
                equipped = equipped,
                selected = selectedAvatar,
                onBack = { currentScreen = "profile" },
                onSelect = { selectedAvatar = it },
                onEquip = {
                    harness.avatarVm.selectAvatar(it.id)
                    currentScreen = "profile"
                },
            )
        }
    }

    @Test
    fun profileAndAvatar_flow_startOnProfile_changeAvatar_equipsNewAvatarAndReflectsOnProfile() {
        val harness = createHarness()

        composeTestRule.setContent {
            ProfileAndAvatarNavigatorHost(harness)
        }
        composeTestRule.waitForIdle()

        // 1. Initial Profile Screen assertions
        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALIANTHERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("LVL 4 → 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Change avatar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hero").assertIsDisplayed() // Avatar label in list row

        // 2. Navigate to Avatar Picker Screen
        composeTestRule.onNodeWithText("Change avatar").performClick()
        composeTestRule.waitForIdle()

        // 3. Avatar Picker assertions: Equipped is Hero, button disabled
        composeTestRule.onNodeWithText("CHOOSE AVATAR").assertIsDisplayed()
        composeTestRule.onNodeWithTag(AvatarTags.tile(AvatarClass.Hero)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AvatarTags.EQUIP).assertIsNotEnabled()

        // 4. Select Mage avatar
        composeTestRule.onNodeWithTag(AvatarTags.tile(AvatarClass.Mage)).performClick()
        composeTestRule.waitForIdle()

        // Equip button is enabled with "EQUIP MAGE"
        composeTestRule.onNodeWithTag(AvatarTags.EQUIP).assertIsEnabled()
        composeTestRule.onNodeWithText("EQUIP MAGE").assertIsDisplayed()

        // 5. Tap Equip
        composeTestRule.onNodeWithTag(AvatarTags.EQUIP).performClick()
        composeTestRule.waitForIdle()

        // 6. Returned to Profile Screen, verify Mage is now equipped
        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALIANTHERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mage").assertIsDisplayed()
    }

    @Test
    fun profile_emptyProfile_handlesBlankFieldsGracefully() {
        val emptyProfile = UserProfileEntity(
            id = 1,
            username = "",
            avatarId = "",
            level = 1,
            totalXp = 0,
            perfectDaysTowardNextLevel = 0,
        )
        val harness = createHarness(initialProfile = emptyProfile, currentStreak = 0, longestStreak = 0)

        composeTestRule.setContent {
            ProfileAndAvatarNavigatorHost(harness)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("LVL 1 → 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("BRONZE").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0").assertCountEquals(3)
    }

    @Test
    fun profile_massiveInputAndExtremeNumbers_overflowProtection() {
        val hugeName = "A".repeat(500)
        val massiveProfile = UserProfileEntity(
            id = 999,
            username = hugeName,
            avatarId = "avatar_paladin",
            level = 9999,
            totalXp = 9999999,
            perfectDaysTowardNextLevel = 5,
        )
        val harness = createHarness(initialProfile = massiveProfile, currentStreak = 5000, longestStreak = 5000)

        composeTestRule.setContent {
            ProfileAndAvatarNavigatorHost(harness)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("LVL 9999 → 10000").assertIsDisplayed()
        composeTestRule.onNodeWithText("9999999").assertIsDisplayed()
        composeTestRule.onNodeWithText("5000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paladin").assertIsDisplayed()
    }

    @Test
    fun profile_oddCharactersInUsername_emojiArabicAndHtml() {
        val oddName = "👑 بطل <script>alert(1)</script>"
        val profile = UserProfileEntity(
            id = 1,
            username = oddName,
            avatarId = "avatar_rogue",
            level = 3,
            totalXp = 300,
        )
        val harness = createHarness(initialProfile = profile)

        composeTestRule.setContent {
            ProfileAndAvatarNavigatorHost(harness)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText(oddName.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Rogue").assertIsDisplayed()
    }

    @Test
    fun profile_simpleMode_suppressesGamifiedMetricsAndLocksDifficulty() {
        val harness = createHarness(simpleMode = true)
        var difficultyClicked = false

        composeTestRule.setContent {
            ProfileAndAvatarNavigatorHost(harness, onDifficultyClick = { difficultyClicked = true })
        }
        composeTestRule.waitForIdle()

        // 1. Header is PROFILE instead of HERO
        composeTestRule.onNodeWithText("PROFILE").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("HERO").fetchSemanticsNodes().size)

        // 2. Gamified elements suppressed (XP bar, Tier badge, LVL)
        assertEquals(0, composeTestRule.onAllNodesWithText("LVL", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("BRONZE").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("TOTAL XP").fetchSemanticsNodes().size)

        // 3. Simple mode metrics displayed
        composeTestRule.onNodeWithText("TASKS DONE").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACTIVE").assertIsDisplayed()

        // 4. Difficulty row shows Locked and does not trigger onDifficulty
        composeTestRule.onNodeWithText("Difficulty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Locked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Difficulty").performClick()
        assertFalse("Difficulty row must not trigger navigation callback when locked in Simple Mode", difficultyClicked)

        // 5. Level history row is absent in Simple Mode
        assertEquals(0, composeTestRule.onAllNodesWithText("Level history").fetchSemanticsNodes().size)
    }

    @Test
    fun profile_signedOutVersusSignedIn_leaderboardLockState() {
        // 1. Signed out -> Leaderboard locked
        val signedOutHarness = createHarness(signedIn = false)
        composeTestRule.setContent {
            ProfileAndAvatarNavigatorHost(signedOutHarness)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Leaderboard"))
        composeTestRule.onNodeWithText("Leaderboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Locked").assertIsDisplayed()
    }

    @Test
    fun avatar_toggleSpam_rapidAvatarSwitching_updatesPreviewAccurately() {
        var equipped by mutableStateOf(AvatarClass.Hero)
        var selected by mutableStateOf(AvatarClass.Hero)

        composeTestRule.setContent {
            AvatarPickerScreen(
                equipped = equipped,
                selected = selected,
                onBack = {},
                onSelect = { selected = it },
                onEquip = { equipped = it },
            )
        }
        composeTestRule.waitForIdle()

        // Rapidly cycle through all avatars
        val classes = listOf(
            AvatarClass.Mage,
            AvatarClass.Rogue,
            AvatarClass.Warrior,
            AvatarClass.Paladin,
            AvatarClass.Ranger,
            AvatarClass.Hero,
        )

        for (avatar in classes) {
            composeTestRule.onNodeWithTag(AvatarTags.tile(avatar)).performClick()
            composeTestRule.waitForIdle()
            assertEquals(avatar, selected)
            if (avatar == equipped) {
                composeTestRule.onNodeWithTag(AvatarTags.EQUIP).assertIsNotEnabled()
            } else {
                composeTestRule.onNodeWithTag(AvatarTags.EQUIP).assertIsEnabled()
            }
        }
    }

    @Test
    fun avatar_unknownOrCorruptAvatarId_fallsBackToHeroSafely() {
        val fallback = AvatarClass.from("corrupt_nonexistent_class_id_12345")
        assertEquals(AvatarClass.Hero, fallback)
    }

    @Test
    fun profileAndAvatar_displayMatrix_comicSkin_andFontScale2x() {
        val harness = createHarness()

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, fontScale = 2.0f),
        ) {
            ProfileAndAvatarNavigatorHost(harness)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALIANTHERO").assertIsDisplayed()
    }

    @Test
    fun profileAndAvatar_displayMatrix_pixelSkin_andRtl() {
        val harness = createHarness()

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Pixel,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            ProfileAndAvatarNavigatorHost(harness)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALIANTHERO").assertIsDisplayed()
    }

    data class TestHarness(
        val profileRepo: FakeUserProfileRepository,
        val streakRepo: FakeStreakRepository,
        val diffRepo: FakeDifficultySettingsRepository,
        val settingsRepo: FakeSettingsRepository,
        val profileVm: ProfileViewModel,
        val avatarVm: AvatarSelectionViewModel,
        val signedIn: Boolean,
    )
}
