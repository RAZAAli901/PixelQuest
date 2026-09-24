package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelThemeAssetFilter
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 24: Canonical category icon component with theme dispatch.
 * In Pixel/Light mode: renders the raw icon tinted with PixelThemeAssetFilter.
 * In Comic mode: renders the icon wrapped inside ComicIconBadge with vibrant container color.
 */
@Composable
fun PixelCategoryIcon(
    category: TaskCategory,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tintColor: Color? = null
) {
    val mode = PixelTheme.mode

    if (mode == ThemeMode.Comic) {
        val containerColor = when (category) {
            TaskCategory.FITNESS -> ComicTokens.BurntOrange
            TaskCategory.HEALTH -> ComicTokens.SkyBlue
            TaskCategory.LEARNING -> ComicTokens.Lavender
            TaskCategory.CHORES -> ComicTokens.GoldAccent
            TaskCategory.OTHER -> ComicTokens.CoralRed
        }
        ComicIconBadge(
            iconResId = category.iconResId,
            contentDescription = category.displayName,
            modifier = modifier,
            badgeSize = size + 10.dp,
            iconSize = size,
            backgroundColor = containerColor,
            iconTint = ComicTokens.SolidBlack
        )
        return
    }

    val filterColor = tintColor ?: PixelTheme.colors.primary
    Image(
        painter = painterResource(id = category.iconResId),
        contentDescription = category.displayName,
        colorFilter = PixelThemeAssetFilter.forTheme(mode, filterColor),
        modifier = modifier.size(size)
    )
}
