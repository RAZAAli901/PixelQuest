package com.pixelquest.app.quran.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.AppColorScheme
import com.pixelquest.app.ui.theme.LocalAppColorScheme
import com.pixelquest.app.ui.theme.PixelTheme

/** The page the reader's text is on. */
enum class ReaderPaper {
    /**
     * The look's own page: dark in Pixel, and in System when the phone is in dark mode, so reading
     * at night follows the theme; light in Light; paper in Comic. The reader's default.
     */
    THEME,

    /** Warm paper and brown ink, in any look: easy on the eyes for long reading. */
    SEPIA
}

/**
 * The look's colours with sepia paper and brown ink in place of its page and text. Everything
 * else (its shapes, its borders' style, Comic's ink) stays the look's own.
 */
internal class SepiaColorScheme(base: AppColorScheme) : AppColorScheme by base {
    override val primary = Color(0xFF8B5A2B)
    override val onPrimary = Color.White

    /** A burnt orange: the bookmark ribbon, warm on the paper (the looks' cyan isn't). */
    override val secondary = Color(0xFFB4532A)
    override val onSecondary = Color.White
    override val background = SEPIA_PAPER
    override val onBackground = Color(0xFF3B2F23)
    override val surface = Color(0xFFEDE0C4)
    override val onSurface = Color(0xFF45372A)
    override val surfaceVariant = Color(0xFFE3D3B0)
    override val onSurfaceVariant = Color(0xFF6B5843)
    override val pixelBorder = Color(0xFFB39B72)
    override val isDark = false

    /** A darker brown than [primary], for text: 6:1 on the paper. */
    override val primaryText: Color get() = Color(0xFF7A4A1E)
}

/** The sepia page. */
internal val SEPIA_PAPER = Color(0xFFF5ECD7)

/**
 * [content] on [paper]: the look's own colours, or sepia's in their place. Either way it's the same
 * composition, so changing the page keeps what's open (the settings sheet the player changed it in).
 */
@Composable
fun ReaderPage(paper: ReaderPaper, content: @Composable () -> Unit) {
    val look = PixelTheme.colors
    val sepia = remember(look) { SepiaColorScheme(look) }
    CompositionLocalProvider(LocalAppColorScheme provides if (paper == ReaderPaper.SEPIA) sepia else look, content = content)
}
