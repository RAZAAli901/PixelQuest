package com.pixelquest.app.darood.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Each member's own colour in a party's chat, for their name on their bubbles, so members tell
 * apart at a glance, as in a group chat. It comes from the user id alone, so it's the same on every
 * phone, in every party, every time; and every colour reads on the theme's bubbles (4.5:1 at least).
 */
object MemberColors {
    /** Bright, for Pixel's dark bubbles. */
    val onDark = listOf(
        Color(0xFF4FC3F7), Color(0xFF81C784), Color(0xFFFFB74D), Color(0xFFCE93D8),
        Color(0xFFF48FB1), Color(0xFF4DD0E1), Color(0xFFAED581), Color(0xFFFF8A65)
    )

    /** Deep, for Light's and Comic's white bubbles. */
    val onLight = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFC62828), Color(0xFF6A1B9A),
        Color(0xFFAD1457), Color(0xFF00695C), Color(0xFFBF360C), Color(0xFF283593)
    )

    fun paletteFor(mode: ThemeMode): List<Color> = if (mode == ThemeMode.Pixel) onDark else onLight

    /**
     * Where [userId] falls in a palette of [size]. String.hashCode is fixed by the language, so
     * this is the same everywhere.
     */
    fun indexOf(userId: String, size: Int): Int = Math.floorMod(userId.hashCode(), size)
}

/** [userId]'s colour in the current theme; the theme's text colour when who posted isn't known. */
@Composable
fun memberColor(userId: String?): Color {
    if (userId == null) return PixelTheme.colors.primaryText
    val palette = MemberColors.paletteFor(PixelTheme.mode)
    return palette[MemberColors.indexOf(userId, palette.size)]
}
