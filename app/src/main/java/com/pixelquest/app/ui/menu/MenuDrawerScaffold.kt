package com.pixelquest.app.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
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
 * @param gesturesEnabled whether a swipe from the left edge opens it; a swipe always closes an open
 *   menu. The app turns it on for the five tabs only, where the menu's icon is too.
 */
@Composable
fun MenuDrawerScaffold(
    controller: MenuDrawerController,
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = controller.isOpen,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalMenuDrawer provides controller) {
        ModalNavigationDrawer(
            drawerState = controller.drawerState,
            gesturesEnabled = gesturesEnabled || controller.isOpen,
            modifier = modifier,
            drawerContent = {
                val style = menuStyle()
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(MenuDrawerWidth)
                        .then(style.sheetBorder?.let { Modifier.border(it, style.sheetShape) } ?: Modifier),
                    drawerShape = style.sheetShape,
                    drawerContainerColor = PixelTheme.colors.background
                ) {
                    // Back closes an open menu instead of leaving the screen. Registered here, in the
                    // sheet composed after the screens, so it comes before the navigation's own Back.
                    BackHandler(enabled = controller.isOpen) { controller.close() }
                    drawerContent()
                }
            },
            content = content
        )
    }
}
