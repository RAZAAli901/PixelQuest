package com.pixelquest.app.ui.menu

import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.R
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

/** The side menu's entries and its ViewModel (docs/ai-coach-compact.md, Phase 2; docs/islamic-hub.md). */
@OptIn(ExperimentalCoroutinesApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
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
            listOf(MenuItemId.ISLAMIC, MenuItemId.PARTIES, MenuItemId.HISTORY, MenuItemId.LEADERBOARD, MenuItemId.ACCOUNT, MenuItemId.SETTINGS),
            MenuItems.all.map { it.id }
        )
    }

    @Test
    fun islamic_isOneEntry_alwaysThere() {
        assertEquals(MenuItems.all, MenuItems.all.visibleIn(MenuContext()))
        assertEquals(MenuItems.all, MenuItems.all.visibleIn(MenuContext(isSimpleMode = true)))
        assertEquals("🌙", MenuItems.Islamic.icon)
    }

    @Test
    fun parties_isItsOwnEntry_outsideTheIslamicSection_owningEveryPartyScreen() {
        assertEquals(MenuItemId.PARTIES, MenuItems.all[1].id)
        assertTrue(MenuItems.Parties.owns("parties"))
        assertTrue(MenuItems.Parties.owns(Screen.PartiesInfo.createRoute("p1")))
        assertFalse("Darood Parties are the Islamic section's", MenuItems.Parties.owns(Screen.DaroodParty.route))
        assertFalse(Routes.isIslamic(Screen.Parties.route))
        assertEquals(R.string.menu_parties, MenuItems.Parties.labelFor(MenuContext(isSimpleMode = true)))
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
            Screen.IslamicHub, Screen.Parties, Screen.TaskHistory, Screen.Leaderboard, Screen.Account, Screen.Settings
        ).map { it.route }
        assertEquals(routes, MenuItems.all.map { it.route })
        assertTrue("the Leaderboard opens like its tab", Routes.isTab(MenuItems.Leaderboard.route))
        assertFalse(Routes.isTab(MenuItems.Islamic.route))
    }

    private fun viewModel(
        saved: SavedStateHandle = SavedStateHandle(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        namaz: FakeNamazRepository? = null,
        myUsername: com.pixelquest.app.darood.data.MyUsername? = null,
        parties: com.pixelquest.app.darood.data.DaroodPartyRepository? = null
    ) = MenuViewModel(
        saved,
        FakeUserProfileRepository(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 450, perfectDaysTowardNextLevel = 2)),
        FakeDifficultySettingsRepository(DifficultySettingsEntity(daysRequiredPerLevel = 7)),
        settings,
        namaz,
        myUsername,
        parties
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

        vm.syncWithRoute(Screen.IslamicHub.route)
        assertEquals(MenuItemId.ISLAMIC, vm.selected.value)

        vm.syncWithRoute(Screen.Quran.route)
        assertEquals("any screen inside the section is Islamic's", MenuItemId.ISLAMIC, vm.selected.value)

        vm.syncWithRoute(Screen.Leaderboard.route)
        assertEquals("from its tab too", MenuItemId.LEADERBOARD, vm.selected.value)

        vm.syncWithRoute(null)
        assertEquals("no route yet changes nothing", MenuItemId.LEADERBOARD, vm.selected.value)
    }

    @Test
    fun theLastEntry_isRestored_fromSavedState() {
        val saved = SavedStateHandle()
        viewModel(saved).select(MenuItemId.ISLAMIC)

        // Android stopped the app in the background; a new ViewModel gets the same saved state.
        val restored = viewModel(saved)

        assertEquals(MenuItemId.ISLAMIC, restored.selected.value)
    }

    @Test
    fun aHighlightSavedUnderAnEntryThatsGone_isDropped() {
        // 1.4 saved "NAMAZ" or "DAROOD", entries now inside the Islamic hub.
        val saved = SavedStateHandle(mapOf("menu_selected" to "NAMAZ"))

        assertNull(viewModel(saved).selected.value)
    }

    @Test
    fun theContext_followsSimpleMode() = runTest {
        val vm = viewModel(settings = FakeSettingsRepository(simpleMode = true))
        backgroundScope.launch { vm.context.collect {} }
        advanceUntilIdle()

        assertEquals(MenuContext(isSimpleMode = true), vm.context.value)
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

    @Test
    fun islamic_hasDaroodsDot_whileAnInvitationWaits() {
        assertFalse(MenuItems.Islamic.dot(MenuContext()))
        assertTrue(MenuItems.Islamic.dot(MenuContext(daroodInviteWaiting = true)))
        assertFalse("only Islamic has one", MenuItems.History.dot(MenuContext(daroodInviteWaiting = true)))
    }

    @Test
    fun parties_hasADot_whileAnInvitationOfAnyKindWaits() {
        assertFalse(MenuItems.Parties.dot(MenuContext()))
        assertTrue(MenuItems.Parties.dot(MenuContext(partyInviteWaiting = true)))
        assertFalse("Islamic's is Darood's alone", MenuItems.Islamic.dot(MenuContext(partyInviteWaiting = true)))
    }

    @Test
    fun aStudyInvitation_dotsParties_notIslamic() = runBlocking {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>(),
            com.pixelquest.app.data.local.AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val viewModels = com.pixelquest.app.testing.TestViewModels()
        try {
            val server = com.pixelquest.app.testing.FakeDaroodServer(me = "me")
            val parties = com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl(
                server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") }
            )
            val study = server.seedParty("Exam group", "u2", type = com.pixelquest.app.party.domain.PartyType.STUDY)
            server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(study, "me", "member", "invited", null, null)
            parties.refreshParties()
            val vm = viewModels.keep(MenuViewModel::class.java) { viewModel(settings = FakeSettingsRepository(), parties = parties) }

            val context = vm.context.first { it.partyInviteWaiting }
            assertFalse(context.daroodInviteWaiting)
        } finally {
            viewModels.clear()
            db.closeAfterWrites()
        }
    }

    @Test
    fun theDot_waitsForDaroodToBeAvailable() = runBlocking {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>(),
            com.pixelquest.app.data.local.AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val viewModels = com.pixelquest.app.testing.TestViewModels()
        try {
            val server = com.pixelquest.app.testing.FakeDaroodServer(me = "me")
            val parties = com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl(
                server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") }
            )
            server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(server.seedParty("Family", "u2"), "me", "member", "invited", null, null)
            parties.refreshParties()
            val settings = FakeSettingsRepository()
            val vm = viewModels.keep(MenuViewModel::class.java) { viewModel(settings = settings, parties = parties) }

            assertTrue(vm.context.first { it.daroodInviteWaiting }.daroodInviteWaiting)

            settings.setDaroodAvailable(false)
            assertFalse(vm.context.first { !it.daroodInviteWaiting }.daroodInviteWaiting)
        } finally {
            viewModels.clear()
            db.closeAfterWrites()
        }
    }
}
