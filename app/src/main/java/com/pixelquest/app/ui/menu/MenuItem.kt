package com.pixelquest.app.ui.menu

import androidx.annotation.StringRes

/** The side menu's entries (docs/ai-coach-compact.md, Phase 2). */
enum class MenuItemId { NAMAZ, HISTORY, LEADERBOARD, ACCOUNT, SETTINGS }

/** What an entry's visibility can depend on. */
data class MenuContext(
    /** The Namaz feature is available in this build and not hidden in Settings. */
    val namazAvailable: Boolean = true,
    /** Simple Mode: plain wording, no game terms. */
    val isSimpleMode: Boolean = false
)

/**
 * One entry in the side menu.
 *
 * @param label the entry's text; [simpleLabel] replaces it in Simple Mode.
 * @param icon a short emoji, like the rest of the app's labels.
 * @param route where it goes ([com.pixelquest.app.ui.navigation.Screen]).
 * @param visibleWhen whether it's shown at all.
 */
data class MenuItem(
    val id: MenuItemId,
    @StringRes val label: Int,
    val icon: String,
    val route: String,
    @StringRes val simpleLabel: Int = label,
    val visibleWhen: (MenuContext) -> Boolean = { true }
) {
    @StringRes
    fun labelFor(context: MenuContext): Int = if (context.isSimpleMode) simpleLabel else label
}

/** The entries [context] shows, in order. */
fun List<MenuItem>.visibleIn(context: MenuContext): List<MenuItem> = filter { it.visibleWhen(context) }
