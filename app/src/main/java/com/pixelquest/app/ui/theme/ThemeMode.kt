package com.pixelquest.app.ui.theme

/**
 * PixelQuest Theming Architecture Modes:
 * - [Pixel]: Classic 8-bit retro dark arcade aesthetic (default).
 * - [Light]: Clean, high-contrast light theme (full design in Day 17).
 * - [Comic]: Bold pop-art comic-book theme with action borders (full design in Days 20-23).
 *
 * Gating policy (Day 20 Step 18):
 * Comic Mode is gated with [isAvailable] = false ("Coming Soon") until screens are restyled on Day 23.
 */
enum class ThemeMode(
    val id: String,
    val displayName: String,
    val isAvailable: Boolean = true
) {
    System("system", "Follow System", isAvailable = true),
    Pixel("pixel", "Retro Pixel (Dark)", isAvailable = true),
    Light("light", "Clean Light", isAvailable = true),
    Comic("comic", "Comic Pop (Coming Soon)", isAvailable = false);

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
