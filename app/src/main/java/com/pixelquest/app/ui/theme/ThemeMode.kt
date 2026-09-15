package com.pixelquest.app.ui.theme

/**
 * PixelQuest Theming Architecture Modes:
 * - [Pixel]: Classic 8-bit retro dark arcade aesthetic (default).
 * - [Light]: Clean, high-contrast light theme (full design in Day 17).
 * - [Comic]: Bold pop-art comic-book theme with action borders (full design in Days 20-23).
 */
enum class ThemeMode(
    val id: String,
    val displayName: String
) {
    System("system", "Follow System"),
    Pixel("pixel", "Retro Pixel (Dark)"),
    Light("light", "Clean Light"),
    Comic("comic", "Comic Pop (Coming Soon)");

    /**
     * Resolves the concrete runtime theme mode based on device dark/light state.
     */
    fun resolveEffective(isSystemInDark: Boolean): ThemeMode = when (this) {
        System -> if (isSystemInDark) Pixel else Light
        Pixel -> Pixel
        Light -> Light
        Comic -> Comic
    }

    companion object {
        fun fromId(id: String?): ThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: Pixel
        }
    }
}
