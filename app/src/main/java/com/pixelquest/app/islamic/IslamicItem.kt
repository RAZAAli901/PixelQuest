package com.pixelquest.app.islamic

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * One card on the Islamic hub (docs/islamic-hub.md): what it's called, what it is in a line, its
 * icon, where it leads, whether it's shown, and how many things wait there.
 *
 * @param id unique among the hub's items, and stable: tests and the hub's tags use it.
 * @param route the screen its card opens, under `islamic/`.
 * @param enabled false hides the card (its feature is off in this build).
 * @param badgeCount things waiting there, such as prayers to mark or invitations; 0 shows no badge.
 * @param badgeLabel how TalkBack says the badge: a plural that takes the count. Null for an item
 *   that never has one.
 */
@Immutable
data class IslamicItem(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: IslamicIcon,
    val route: String,
    val enabled: Boolean = true,
    val badgeCount: Int = 0,
    @PluralsRes val badgeLabel: Int? = null
) {
    val hasBadge: Boolean get() = badgeCount > 0
}

/**
 * An item's icon: an emoji, like the rest of the app's labels, with drawn versions for Pixel and
 * Comic where the item has them (as the side menu's Darood entry does).
 */
@Immutable
data class IslamicIcon(
    val emoji: String,
    @DrawableRes val pixel: Int? = null,
    @DrawableRes val comic: Int? = null
) {
}
