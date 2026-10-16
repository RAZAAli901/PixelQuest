package com.pixelquest.app.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelTheme

/** How wide the side menu is: room for its entries without covering the whole screen. */
val MenuDrawerWidth = 300.dp

/**
 * The app's frame with the side menu (docs/ai-coach-compact.md, Phase 2): the menu slides in over
 * [content], and the screens inside can open it through [LocalMenuDrawer].
 *
 * @param gesturesEnabled whether a swipe opens or closes it; a swipe always closes an open menu.
 */
@Composable
fun MenuDrawerScaffold(
    controller: MenuDrawerController,
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = controller.isOpen,
    content: @Composable () -> Unit
) {
    // Back closes an open menu before it leaves the screen.
    BackHandler(enabled = controller.isOpen) { controller.close() }
    CompositionLocalProvider(LocalMenuDrawer provides controller) {
        ModalNavigationDrawer(
            drawerState = controller.drawerState,
            gesturesEnabled = gesturesEnabled || controller.isOpen,
            modifier = modifier,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(MenuDrawerWidth),
                    drawerContainerColor = PixelTheme.colors.background,
                    content = drawerContent
                )
            },
            content = content
        )
    }
}
