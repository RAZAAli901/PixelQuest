package com.pixelquest.app.islamic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * One item on the hub: its icon, with its badge across from it, then its title and what it is in a
 * line. The whole card opens the item. In Simple Mode it's a plain row instead: icon, words, badge.
 */
@Composable
fun IslamicItemCard(
    item: IslamicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false
) {
    val colors = PixelTheme.colors
    IslamicCardFrame(
        isSimpleMode = isSimpleMode,
        onClick = onClick,
        modifier = modifier.testTag(IslamicTestTags.card(item.id))
    ) {
        if (isSimpleMode) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IslamicItemIcon(icon = item.icon, isSimpleMode = true)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = stringResource(item.titleRes), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    // The titles' plain font, not the theme's pixel body one: Simple Mode is the quiet look.
                    Text(
                        text = stringResource(item.descriptionRes),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MaterialTheme.typography.titleSmall.fontFamily),
                        color = colors.onSurfaceVariant
                    )
                }
                if (item.hasBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IslamicBadge(count = item.badgeCount, isSimpleMode = true)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    IslamicItemIcon(icon = item.icon, isSimpleMode = false)
                    Spacer(modifier = Modifier.weight(1f))
                    if (item.hasBadge) IslamicBadge(count = item.badgeCount)
                }
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = inkOnPanel(colors.primaryText)
                )
                Text(
                    text = stringResource(item.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurface
                )
            }
        }
    }
}

/**
 * A hub card's frame. Pixel, Comic and Light use the app's panels ([PixelCard]: Pixel's frame,
 * Comic's ink and flat shadow, Light's daylight panel, as Home's cards). Simple Mode, in any theme,
 * is the plain look its side menu has: a softly rounded block, no border, no shadow.
 *
 * @param onClick makes the whole frame one button; null for a card that opens nothing.
 */
@Composable
internal fun IslamicCardFrame(
    isSimpleMode: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    if (isSimpleMode) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = modifier
                .clip(shape)
                .background(PixelTheme.colors.surface, shape)
                .then(tap)
                .padding(14.dp)
        ) { content() }
    } else {
        PixelCard(contentPadding = 14.dp, modifier = modifier.then(tap)) { content() }
    }
}

/** The item's drawn icon in Pixel and Comic where it has one, else its emoji. */
@Composable
private fun IslamicItemIcon(icon: IslamicIcon, isSimpleMode: Boolean) {
    val drawn = icon.drawableFor(PixelTheme.mode, isSimpleMode)
    if (drawn != null) {
        Icon(
            painter = painterResource(drawn),
            contentDescription = null,
            tint = inkOnPanel(PixelTheme.colors.primaryText),
            modifier = Modifier.size(32.dp)
        )
    } else {
        Text(text = icon.emoji, fontSize = 28.sp)
    }
}

/**
 * How many things wait in the item: up to 99, then 99+. Its shape follows the look: a chamfered
 * chip in Pixel, an inked pill in Comic, a plain pill in Light and Simple Mode.
 */
@Composable
private fun IslamicBadge(count: Int, isSimpleMode: Boolean = false) {
    val colors = PixelTheme.colors
    val shape: Shape = when {
        isSimpleMode -> RoundedCornerShape(50)
        PixelTheme.mode == ThemeMode.Pixel || PixelTheme.mode == ThemeMode.System -> CutCornerShape(4.dp)
        else -> RoundedCornerShape(50)
    }
    val ink = if (!isSimpleMode && PixelTheme.mode == ThemeMode.Comic) {
        Modifier.border(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack, shape)
    } else {
        Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
            .background(colors.secondary, shape)
            .then(ink)
            .padding(horizontal = 6.dp)
            .testTag(IslamicTestTags.BADGE)
    ) {
        Text(
            text = if (count > MAX_SHOWN) "$MAX_SHOWN+" else count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSecondary
        )
    }
}

private const val MAX_SHOWN = 99
