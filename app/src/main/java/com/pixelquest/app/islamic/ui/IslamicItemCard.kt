package com.pixelquest.app.islamic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape

/** An item's icon on Deen, by its id: the book, the group, the moon; a sparkle for one to come. */
fun deenIcon(id: String): ImageVector = when (id) {
    "quran" -> Icons.AutoMirrored.Rounded.MenuBook
    "darood" -> Icons.Rounded.Groups
    "namaz" -> Icons.Rounded.Brightness3
    else -> Icons.Rounded.AutoAwesome
}

/** An item's tile colour on Deen, by its id. */
fun deenColor(id: String): Color = when (id) {
    "quran" -> PqColors.Lilac
    "darood" -> PqColors.Amber
    "namaz" -> PqColors.Sky
    else -> PqColors.Rose
}

/**
 * One item on Deen (UI v2, mock 2D): its icon, title and line, its badge while something waits, and
 * a bar for progress (the Quran's place). The whole tile opens the item.
 *
 * To TalkBack the tile is one button, read as a sentence: its title, its description, and what its
 * badge counts ("2 prayers to mark"), never a bare number.
 *
 * @param body the line under the title in place of the item's description ("Continue Al-Baqara").
 */
@Composable
fun IslamicItemCard(
    item: IslamicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false,
    body: String? = null,
    progress: Float? = null,
) {
    val spoken = islamicCardSpoken(item, body)
    PqCard(
        modifier
            .heightIn(min = if (isSimpleMode) 0.dp else 150.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                role = Role.Button
            }
            .testTag(IslamicTestTags.card(item.id)),
        bg = deenColor(item.id),
        onClick = onClick,
        onClickLabel = stringResource(R.string.islamic_card_open),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Icon(deenIcon(item.id), null, tint = PqColors.Ink, modifier = Modifier.size(30.dp))
                Spacer(Modifier.weight(1f))
                if (item.hasBadge) IslamicBadge(item.badgeCount)
            }
            Text(stringResource(item.titleRes), style = PqType.BodyStrong.copy(fontSize = 16.bsp))
            Text(body ?: stringResource(item.descriptionRes), style = PqType.Meta.copy(color = PqColors.Ink))
            if (progress != null) {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.fillMaxWidth().height(
                        10.dp,
                    ).clip(pqShape(5.dp)).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(5.dp)),
                ) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(10.dp).background(PqColors.Ink))
                }
            }
        }
    }
}

/** What TalkBack says for [item]'s tile: its title, its line, and its badge in words. */
@Composable
internal fun islamicCardSpoken(item: IslamicItem, body: String? = null): String {
    val title = stringResource(item.titleRes)
    val description = body ?: stringResource(item.descriptionRes)
    if (!item.hasBadge) return stringResource(R.string.islamic_card_spoken, title, description)
    val badge = item.badgeLabel?.let { pluralStringResource(it, item.badgeCount, item.badgeCount) } ?: item.badgeCount.toString()
    return stringResource(R.string.islamic_card_spoken_badge, title, description, badge)
}

/** How many things wait in the item: up to 99, then 99+. The tile says what it counts. */
@Composable
internal fun IslamicBadge(count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 26.dp, minHeight = 26.dp)
            .clip(PqCircle)
            .background(PqColors.Red)
            .border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
            .padding(horizontal = 6.dp)
            .testTag(IslamicTestTags.BADGE)
            // The tile says what it counts; the number alone would be read out of context.
            .clearAndSetSemantics {},
    ) {
        Text(badgeText(count), style = PqType.Button.copy(color = PqColors.OnRed))
    }
}

/** A badge's number as shown: up to 99, then 99+. */
internal fun badgeText(count: Int): String = if (count > MAX_SHOWN) "$MAX_SHOWN+" else count.toString()

private const val MAX_SHOWN = 99
