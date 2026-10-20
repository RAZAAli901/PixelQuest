package com.pixelquest.app.ui.menu

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Opens and closes the side menu. Wraps Material's [DrawerState] (which also animates it and
 * survives rotation) so screens can ask for the menu without a coroutine scope of their own.
 */
@Stable
class MenuDrawerController(
    val drawerState: DrawerState,
    private val scope: CoroutineScope
) {
    val isOpen: Boolean get() = drawerState.isOpen

    fun open() {
        scope.launch { drawerState.open() }
    }

    fun close() {
        scope.launch { drawerState.close() }
    }
}

@Composable
fun rememberMenuDrawerController(initial: DrawerValue = DrawerValue.Closed): MenuDrawerController {
    val drawerState = rememberDrawerState(initial)
    val scope = rememberCoroutineScope()
    return remember(drawerState, scope) { MenuDrawerController(drawerState, scope) }
}

/**
 * The side menu, for the screens' three-line icon. Null where there's no menu (previews, tests,
 * screens outside the app's frame), and then the icon isn't shown.
 */
val LocalMenuDrawer = staticCompositionLocalOf<MenuDrawerController?> { null }

/** The side menu's data: who's playing, for its header, and which entry was chosen last. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    userProfileRepository: UserProfileRepository,
    difficultySettingsRepository: DifficultySettingsRepository,
    settingsRepository: SettingsRepository,
    namazRepository: NamazRepository? = null,
    myUsername: com.pixelquest.app.darood.data.MyUsername? = null,
    daroodParties: com.pixelquest.app.darood.data.DaroodPartyRepository? = null
) : ViewModel() {

    /** XP from today's prayers while the Namaz Tracker is on (by the calendar day), else null. */
    private val namazXpToday = namazRepository?.let { repo ->
        repo.observeSettings().map { it.enabled && it.xpEnabled }.distinctUntilChanged().flatMapLatest { on ->
            if (!on) flowOf(null) else repo.observeDay(java.time.LocalDate.now()).map { day -> day.sumOf { it.xpAwarded + it.bonusXp } }
        }
    } ?: flowOf(null)

    val header: StateFlow<MenuHeaderState> = combine(
        userProfileRepository.getProfile(),
        difficultySettingsRepository.getCurrentDifficulty(),
        settingsRepository.simpleModeEnabled,
        namazXpToday,
        myUsername?.username ?: flowOf(null)
    ) { profile, difficulty, simple, namazXp, username ->
        MenuHeaderState(
            name = profile?.username.orEmpty(),
            avatarId = profile?.avatarId.orEmpty(),
            level = profile?.level ?: 1,
            totalXp = profile?.totalXp ?: 0,
            perfectDays = profile?.perfectDaysTowardNextLevel ?: 0,
            daysPerLevel = difficulty?.daysRequiredPerLevel ?: 7,
            isSimpleMode = simple,
            namazXpToday = namazXp,
            username = username?.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MenuHeaderState())

    /**
     * What decides what the entries say: Simple Mode; a Darood invitation waiting (while Darood is
     * available), for Islamic's dot; and an invitation of any kind waiting, for Parties'.
     */
    val context: StateFlow<MenuContext> = combine(
        settingsRepository.simpleModeEnabled,
        settingsRepository.daroodAvailable,
        daroodParties?.observeMyParties()?.map { parties -> parties.filter { it.myStatus == com.pixelquest.app.party.domain.MemberStatus.INVITED } }
            ?: flowOf(emptyList())
    ) { simple, darood, invited ->
        MenuContext(
            isSimpleMode = simple,
            daroodInviteWaiting = darood && invited.any { it.type == com.pixelquest.app.party.domain.PartyType.DAROOD },
            partyInviteWaiting = invited.isNotEmpty()
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MenuContext())

    // Kept in the saved state, so the last entry opened is still highlighted after a rotation, or
    // after Android stopped the app in the background and the player came back to it.
    private val _selected = MutableStateFlow(
        savedStateHandle.get<String>(SELECTED_KEY)?.let { name -> MenuItemId.entries.firstOrNull { it.name == name } }
    )
    val selected: StateFlow<MenuItemId?> = _selected.asStateFlow()

    fun select(id: MenuItemId) {
        _selected.value = id
        savedStateHandle[SELECTED_KEY] = id.name
    }

    /** Back on a tab, no menu entry is the current place. */
    fun clearSelection() {
        _selected.value = null
        savedStateHandle[SELECTED_KEY] = null
    }

    /** Follows where the player is: the entry that owns [route] (any Islamic screen is Islamic's), or none (a tab, a form). */
    fun syncWithRoute(route: String?) {
        if (route == null) return
        val here = MenuItems.all.firstOrNull { it.owns(route) }
        if (here != null) select(here.id) else clearSelection()
    }

    private companion object {
        const val SELECTED_KEY = "menu_selected"
    }
}
