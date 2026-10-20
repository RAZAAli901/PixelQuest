package com.pixelquest.app.islamic.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * One item on the hub: its icon, with its badge across from it, then its title and what it is in a
 * line. The whole card opens the item.
 */
@Composable
fun IslamicItemCard(
    item: IslamicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false
) {
    val colors = PixelTheme.colors
    PixelCard(
        contentPadding = 14.dp,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(IslamicTestTags.card(item.id))
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                IslamicItemIcon(icon = item.icon, isSimpleMode = isSimpleMode)
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

/** How many things wait in the item: up to 99, then 99+. */
@Composable
private fun IslamicBadge(count: Int) {
    val colors = PixelTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
            .background(colors.secondary, RoundedCornerShape(12.dp))
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
