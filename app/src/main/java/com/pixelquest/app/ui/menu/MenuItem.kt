package com.pixelquest.app.ui.menu

import androidx.annotation.StringRes
import com.pixelquest.app.R
import com.pixelquest.app.ui.navigation.Screen

/** The side menu's entries (docs/ai-coach-compact.md, Phase 2). */
enum class MenuItemId { NAMAZ, DAROOD, HISTORY, LEADERBOARD, ACCOUNT, SETTINGS }

/** What an entry's visibility can depend on. */
data class MenuContext(
    /** The Namaz feature is available in this build and not hidden in Settings. */
    val namazAvailable: Boolean = true,
    /** Simple Mode: plain wording, no game terms. */
    val isSimpleMode: Boolean = false,
    /** The Namaz Tracker's own switch is on. */
    val namazEnabled: Boolean = false,
    /** Darood Party's feature flag is on (SettingsRepository.daroodAvailable). */
    val daroodAvailable: Boolean = true
)

/**
 * One entry in the side menu.
 *
 * @param label the entry's text; [simpleLabel] replaces it in Simple Mode.
 * @param icon a short emoji, like the rest of the app's labels.
 * @param route where it goes ([com.pixelquest.app.ui.navigation.Screen]).
 * @param visibleWhen whether it's shown at all.
 * @param status a short state shown at the row's end, or null for none.
 */
data class MenuItem(
    val id: MenuItemId,
    @StringRes val label: Int,
    val icon: String,
    val route: String,
    @StringRes val simpleLabel: Int = label,
    val visibleWhen: (MenuContext) -> Boolean = { true },
    val status: (MenuContext) -> Int? = { null }
) {
    @StringRes
    fun labelFor(context: MenuContext): Int = if (context.isSimpleMode) simpleLabel else label
}

/** The entries [context] shows, in order. */
fun List<MenuItem>.visibleIn(context: MenuContext): List<MenuItem> = filter { it.visibleWhen(context) }

/** The side menu's entries, in order. */
object MenuItems {
    val Namaz = MenuItem(
        id = MenuItemId.NAMAZ,
        label = R.string.menu_namaz,
        icon = "🕌",
        route = Screen.Namaz.route,
        // Shown while the tracker is off too: it's the way to its switch. It says when it's on.
        visibleWhen = { it.namazAvailable },
        status = { if (it.namazEnabled) R.string.namaz_switch_on else null }
    )

    /** Darood Party (docs/darood-design.md): pooling Darood with friends. No game terms, so no Simple Mode label. */
    val Darood = MenuItem(
        id = MenuItemId.DAROOD,
        label = R.string.menu_darood,
        icon = "📿",
        route = Screen.DaroodParty.route,
        visibleWhen = { it.daroodAvailable }
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

    val all: List<MenuItem> = listOf(Namaz, Darood, History, Leaderboard, Account, Settings)
}
