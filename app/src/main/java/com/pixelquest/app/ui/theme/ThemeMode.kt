package com.pixelquest.app.ui.theme

/**
 * PixelQuest Theming Architecture Modes:
 * - [System]: Follows the device's dark setting: Pixel when dark, Light when light. Never Comic.
 * - [Pixel]: Classic 8-bit retro dark arcade aesthetic (default).
 * - [Light]: Clean, high-contrast light theme (Day 17).
 * - [Comic]: Bold pop-art comic-book theme with ink borders (Days 20-23).
 *
 * Every mode is available ([isAvailable] = true). Comic was gated until Day 23 and unlocked then.
 */
enum class ThemeMode(
    val id: String,
    val displayName: String,
    val isAvailable: Boolean = true
) {
    System("system", "Follow System", isAvailable = true),
    Pixel("pixel", "Retro Pixel (Dark)", isAvailable = true),
    Light("light", "Clean Light", isAvailable = true),
    Comic("comic", "Comic Pop", isAvailable = true);

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
            val cleanId = id?.trim()
            return entries.firstOrNull { it.id.equals(cleanId, ignoreCase = true) } ?: Pixel
        }
    }
}
