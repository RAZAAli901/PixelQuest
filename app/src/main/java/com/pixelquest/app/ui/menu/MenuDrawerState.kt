package com.pixelquest.app.ui.menu

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
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

/** Which menu entry was chosen last, so the drawer can show it as selected. */
@HiltViewModel
class MenuViewModel @Inject constructor() : ViewModel() {

    private val _selected = MutableStateFlow<MenuItemId?>(null)
    val selected: StateFlow<MenuItemId?> = _selected.asStateFlow()

    fun select(id: MenuItemId) {
        _selected.value = id
    }

    /** Back on a tab, no menu entry is the current place. */
    fun clearSelection() {
        _selected.value = null
    }
}
