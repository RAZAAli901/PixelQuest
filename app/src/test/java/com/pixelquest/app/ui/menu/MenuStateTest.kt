package com.pixelquest.app.ui.menu

import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.R
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The side menu's entries and its ViewModel (docs/ai-coach-compact.md, Phase 2). */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuStateTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theEntries_inOrder() {
        assertEquals(
            listOf(MenuItemId.NAMAZ, MenuItemId.DAROOD, MenuItemId.HISTORY, MenuItemId.LEADERBOARD, MenuItemId.ACCOUNT, MenuItemId.SETTINGS),
            MenuItems.all.map { it.id }
        )
    }

    @Test
    fun namaz_showsOnlyWhileAvailable() {
        assertTrue(MenuItemId.NAMAZ in MenuItems.all.visibleIn(MenuContext(namazAvailable = true)).map { it.id })

        val without = MenuItems.all.visibleIn(MenuContext(namazAvailable = false)).map { it.id }

        assertFalse(MenuItemId.NAMAZ in without)
        assertEquals("the rest stay", 5, without.size)
    }

    @Test
    fun simpleMode_usesPlainWords() {
        assertEquals(R.string.menu_history, MenuItems.History.labelFor(MenuContext()))
        assertEquals(R.string.menu_history_simple, MenuItems.History.labelFor(MenuContext(isSimpleMode = true)))
        assertEquals("no simple wording needed", R.string.menu_settings, MenuItems.Settings.labelFor(MenuContext(isSimpleMode = true)))
    }

    @Test
    fun everyEntry_leadsSomewhereReal() {
        val routes = listOf(
            Screen.Namaz, Screen.DaroodParty, Screen.TaskHistory, Screen.Leaderboard, Screen.Account, Screen.Settings
        ).map { it.route }
        assertEquals(routes, MenuItems.all.map { it.route })
        assertTrue("the Leaderboard opens like its tab", Routes.isTab(MenuItems.Leaderboard.route))
        assertFalse(Routes.isTab(MenuItems.Namaz.route))
        assertFalse(Routes.isTab(MenuItems.Darood.route))
    }

    private fun viewModel(
        saved: SavedStateHandle = SavedStateHandle(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        namaz: FakeNamazRepository? = null,
        myUsername: com.pixelquest.app.darood.data.MyUsername? = null
    ) = MenuViewModel(
        saved,
        FakeUserProfileRepository(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 450, perfectDaysTowardNextLevel = 2)),
        FakeDifficultySettingsRepository(DifficultySettingsEntity(daysRequiredPerLevel = 7)),
        settings,
        namaz,
        myUsername
    )

    @Test
    fun theHeader_comesFromTheProfile() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.header.collect {} }
        advanceUntilIdle()

        val header = vm.header.value
        assertEquals("Aria", header.name)
        assertEquals(3, header.level)
        assertEquals(450, header.totalXp)
        assertEquals(2f / 7f, header.levelProgress, 0.001f)
    }

    @Test
    fun choosingAnEntry_selectsIt_andATabClearsIt() {
        val vm = viewModel()
        assertNull(vm.selected.value)

        vm.select(MenuItemId.ACCOUNT)
        assertEquals(MenuItemId.ACCOUNT, vm.selected.value)

        vm.syncWithRoute(Screen.Home.route)
        assertNull(vm.selected.value)
    }

    @Test
    fun theHighlight_followsTheScreen() {
        val vm = viewModel()

        vm.syncWithRoute(Screen.Namaz.route)
        assertEquals(MenuItemId.NAMAZ, vm.selected.value)

        vm.syncWithRoute(Screen.Leaderboard.route)
        assertEquals("from its tab too", MenuItemId.LEADERBOARD, vm.selected.value)

        vm.syncWithRoute(null)
        assertEquals("no route yet changes nothing", MenuItemId.LEADERBOARD, vm.selected.value)
    }

    @Test
    fun theLastEntry_isRestored_fromSavedState() {
        val saved = SavedStateHandle()
        viewModel(saved).select(MenuItemId.NAMAZ)

        // Android stopped the app in the background; a new ViewModel gets the same saved state.
        val restored = viewModel(saved)

        assertEquals(MenuItemId.NAMAZ, restored.selected.value)
    }

    @Test
    fun theContext_followsNamazAndSimpleMode() = runTest {
        val namaz = MutableStateFlow(true)
        val settings = object : FakeSettingsRepository(simpleMode = true) {
            override val namazAvailable: Flow<Boolean> = namaz
        }
        val vm = viewModel(settings = settings)
        backgroundScope.launch { vm.context.collect {} }
        advanceUntilIdle()

        assertEquals(MenuContext(namazAvailable = true, isSimpleMode = true), vm.context.value)

        namaz.value = false
        advanceUntilIdle()
        assertFalse(vm.context.value.namazAvailable)
    }

    @Test
    fun theNamazEntry_saysOn_whileTheTrackerIs_andStaysWhileItsOff() = runTest {
        val namaz = FakeNamazRepository()
        val vm = viewModel(namaz = namaz)
        backgroundScope.launch { vm.context.collect {} }
        advanceUntilIdle()

        assertFalse(vm.context.value.namazEnabled)
        assertNull(MenuItems.Namaz.status(vm.context.value))
        assertTrue(MenuItemId.NAMAZ in MenuItems.all.visibleIn(vm.context.value).map { it.id })

        namaz.saveSettings(namaz.getSettings().copy(enabled = true))
        advanceUntilIdle()
        assertTrue(vm.context.value.namazEnabled)
        assertEquals(com.pixelquest.app.R.string.namaz_switch_on, MenuItems.Namaz.status(vm.context.value))
    }

    @Test
    fun theHeader_showsTodaysPrayerXp_whileTheTrackerIsOn() = runTest {
        val namaz = FakeNamazRepository()
        val vm = viewModel(namaz = namaz)
        backgroundScope.launch { vm.header.collect {} }
        advanceUntilIdle()
        assertNull(vm.header.value.namazXpToday)

        namaz.saveSettings(namaz.getSettings().copy(enabled = true))
        namaz.logPrayer(java.time.LocalDate.now(), com.pixelquest.app.domain.namaz.Prayer.FAJR, prayedAtMillis = 0, xpAwarded = 10, bonusXp = 30)
        advanceUntilIdle()
        assertEquals(40, vm.header.value.namazXpToday)

        namaz.saveSettings(namaz.getSettings().copy(xpEnabled = false))
        advanceUntilIdle()
        assertNull(vm.header.value.namazXpToday)
    }

    @Test
    fun theHeader_showsTheSignedInAccountsUsername() = runTest {
        val auth = com.pixelquest.app.auth.FakeAuthRepository()
        val cache = com.pixelquest.app.testing.FakeUsernameCache()
        val vm = viewModel(myUsername = com.pixelquest.app.darood.data.MyUsername(auth, cache))
        backgroundScope.launch { vm.header.collect {} }
        advanceUntilIdle()
        assertNull(vm.header.value.username)

        auth.userFlow.value = com.pixelquest.app.auth.AuthUser(id = "aria-id", email = null, displayName = null)
        cache.save("aria-id", com.pixelquest.app.darood.domain.Username.parse("aria_k")!!)
        advanceUntilIdle()
        assertEquals("aria_k", vm.header.value.username)

        auth.userFlow.value = null
        advanceUntilIdle()
        assertNull(vm.header.value.username)
    }
}
