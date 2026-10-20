package com.pixelquest.app.ui.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pixelquest.app.R
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * The side menu's entries (docs/ai-coach-compact.md, Phase 2). Namaz Tracker and Darood Party moved
 * into the Islamic hub (docs/islamic-hub.md), so a highlight saved under their old ids is dropped.
 */
enum class MenuItemId { ISLAMIC, HISTORY, LEADERBOARD, ACCOUNT, SETTINGS }

/** What an entry's visibility, status and dot can depend on. */
data class MenuContext(
    /** Simple Mode: plain wording, no game terms. */
    val isSimpleMode: Boolean = false,
    /** A Darood Party invitation is waiting for an answer, while Darood is available. */
    val daroodInviteWaiting: Boolean = false
)

/**
 * One entry in the side menu.
 *
 * @param label the entry's text; [simpleLabel] replaces it in Simple Mode.
 * @param icon a short emoji, like the rest of the app's labels.
 * @param route where it goes ([com.pixelquest.app.ui.navigation.Screen]).
 * @param visibleWhen whether it's shown at all.
 * @param status a short state shown at the row's end, or null for none.
 * @param pixelIcon, comicIcon drawn icons for the Pixel and Comic themes, in place of [icon] there.
 * @param dot whether a dot at the row's end says something is waiting there.
 * @param owns whether a route is this entry's place, for the highlight: by default only [route].
 */
data class MenuItem(
    val id: MenuItemId,
    @StringRes val label: Int,
    val icon: String,
    val route: String,
    @StringRes val simpleLabel: Int = label,
    val visibleWhen: (MenuContext) -> Boolean = { true },
    val status: (MenuContext) -> Int? = { null },
    @DrawableRes val pixelIcon: Int? = null,
    @DrawableRes val comicIcon: Int? = null,
    val dot: (MenuContext) -> Boolean = { false },
    val owns: (route: String) -> Boolean = { it == route }
) {
    @StringRes
    fun labelFor(context: MenuContext): Int = if (context.isSimpleMode) simpleLabel else label

    /**
     * The drawn icon for [mode], or null to show the emoji [icon]. Light and Simple Mode keep the
     * emoji, like every other entry there.
     */
    @DrawableRes
    fun drawableFor(mode: ThemeMode, isSimpleMode: Boolean): Int? = when {
        isSimpleMode -> null
        mode == ThemeMode.Comic -> comicIcon
        mode == ThemeMode.Pixel || mode == ThemeMode.System -> pixelIcon
        else -> null
    }
}

/** The entries [context] shows, in order. */
fun List<MenuItem>.visibleIn(context: MenuContext): List<MenuItem> = filter { it.visibleWhen(context) }

/** The side menu's entries, in order. */
object MenuItems {
    /**
     * The Islamic section (docs/islamic-hub.md): the hub of Namaz, Darood Party and the Quran, in
     * place of their own entries. Always there, since the Quran always is. It stays highlighted on
     * every screen inside, and keeps Darood's dot for a waiting invitation. No game terms, so no
     * Simple Mode label.
     */
    val Islamic = MenuItem(
        id = MenuItemId.ISLAMIC,
        label = R.string.menu_islamic,
        icon = "🌙",
        route = Screen.IslamicHub.route,
        pixelIcon = R.drawable.ic_islamic_pixel,
        comicIcon = R.drawable.ic_islamic_comic,
        dot = { it.daroodInviteWaiting },
        owns = Routes::isIslamic
    )

    // Shortcuts to screens that are otherwise a few taps deep (Profile, then Settings, then Account).
    val History = MenuItem(
        id = MenuItemId.HISTORY,
        label = R.string.menu_history,
        simpleLabel = R.string.menu_history_simple,
        icon = "📜",
        route = Screen.TaskHistory.route
    )

    val Leaderboard = MenuItem(
        id = MenuItemId.LEADERBOARD,
        label = R.string.menu_leaderboard,
        icon = "🏆",
        route = Screen.Leaderboard.route
    )

    val Account = MenuItem(
        id = MenuItemId.ACCOUNT,
        label = R.string.menu_account,
        icon = "👤",
        route = Screen.Account.route
    )

    val Settings = MenuItem(
        id = MenuItemId.SETTINGS,
        label = R.string.menu_settings,
        icon = "⚙️",
        route = Screen.Settings.route
    )

    val all: List<MenuItem> = listOf(Islamic, History, Leaderboard, Account, Settings)
}
