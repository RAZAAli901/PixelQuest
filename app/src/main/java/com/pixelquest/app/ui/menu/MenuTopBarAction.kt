package com.pixelquest.app.ui.menu

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * The three-line icon at the top left of a screen that opens the side menu. Shows nothing where
 * there's no menu ([LocalMenuDrawer] is null: previews, tests, screens outside the app's frame).
 */
@Composable
fun MenuTopBarAction(modifier: Modifier = Modifier) {
    val menu = LocalMenuDrawer.current ?: return
    val isComic = PixelTheme.mode == ThemeMode.Comic
    IconButton(onClick = menu::open, modifier = modifier) {
        Icon(
            painter = painterResource(if (isComic) R.drawable.ic_menu_comic else R.drawable.ic_menu_pixel),
            contentDescription = stringResource(R.string.menu_open),
            tint = if (isComic) ComicTokens.SolidBlack else PixelTheme.colors.primaryText,
            modifier = Modifier.size(24.dp)
        )
    }
}
