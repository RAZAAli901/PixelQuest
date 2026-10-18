package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/** How the Namaz screen looks in the current theme, like the side menu's MenuStyle. */
@Immutable
data class NamazStyle(
    /** The next-prayer card's panel. */
    val heroPanel: PixelPanelVariant,
    /** Room inside a card, clear of the theme's frame. */
    val cardPadding: Dp,
    /** Each button state's accent: its ring, mark and label. */
    val pending: Color,
    val prayed: Color,
    val missed: Color,
    val locked: Color,
    /** Inside a prayed ring. */
    val prayedFill: Color,
    /** The ✓ and ✕ on a ring; null draws them in the state's accent. */
    val mark: Color?,
    val ringStroke: Dp,
    /** Section headings: "Your week", "Today's prayers". */
    val heading: TextStyle,
    val uppercaseHeadings: Boolean,
    /** The day strip's tabs. */
    val chipShape: Shape,
    val chipBorderWidth: Dp,
    val chipBorder: Color,
    val chipFill: Color,
    val chipSelectedFill: Color,
    val chipSelectedBorder: Color,
    /** A flat black shadow under the chosen day, like Comic's panels. */
    val chipShadow: Boolean,
    /** The week's dots. Missed is quieter than on the buttons, so a new player's week isn't a wall of red. */
    val dotPrayed: Color,
    val dotMissed: Color,
    val dotOutline: Color,
    /** Game touches: the streak's 🔥, and from Phase 6 the XP a prayer earns. Off in Simple Mode. */
    val playful: Boolean = true
)

/** Simple Mode on the Namaz screen, provided once at its root (NamazContent). */
val LocalNamazSimpleMode = staticCompositionLocalOf { false }

/**
 * The Namaz screen's look: the theme's own, or in Simple Mode the plain one in any theme. Dark
 * mode is the Pixel theme (System follows it on a dark phone); Light and Simple Mode are plain.
 */
@Composable
fun namazStyle(): NamazStyle = when {
    LocalNamazSimpleMode.current -> plainNamazStyle(playful = false)
    PixelTheme.mode == ThemeMode.Comic -> comicNamazStyle()
    PixelTheme.mode == ThemeMode.Light -> plainNamazStyle()
    else -> pixelNamazStyle()
}

/**
 * Pixel: the dark navy panel for the next prayer, room inside the pixel frames, thick rings in
 * gold (pending), arcade green (prayed) and red (missed), and square day tabs.
 */
@Composable
private fun pixelNamazStyle(): NamazStyle {
    val colors = PixelTheme.colors
    return NamazStyle(
        heroPanel = PixelPanelVariant.BLUE,
        cardPadding = 20.dp,
        pending = colors.primary,
        prayed = colors.tertiary,
        missed = colors.error,
        locked = colors.onSurfaceVariant,
        prayedFill = colors.tertiary.copy(alpha = 0.25f),
        mark = null,
        ringStroke = 4.dp,
        heading = MaterialTheme.typography.titleMedium,
        uppercaseHeadings = false,
        chipShape = RectangleShape,
        chipBorderWidth = 2.dp,
        chipBorder = colors.pixelBorder,
        chipFill = colors.surface,
        chipSelectedFill = colors.primary.copy(alpha = 0.18f),
        chipSelectedBorder = colors.primary,
        chipShadow = false,
        dotPrayed = colors.tertiary,
        dotMissed = colors.onSurfaceVariant.copy(alpha = 0.5f),
        dotOutline = colors.onSurfaceVariant
    )
}

/**
 * Comic: black ink rings and marks, coral for prayed (a bright coral fill inside a darker coral
 * ring, so its label stays readable on white), grey for missed, headings in the comic font, and
 * day tabs outlined in ink with the chosen one a coral pill on a flat black shadow.
 */
@Composable
private fun comicNamazStyle(): NamazStyle = NamazStyle(
    heroPanel = PixelPanelVariant.BEIGE,
    cardPadding = 16.dp,
    pending = ComicTokens.SolidBlack,
    prayed = ComicTokens.CoralInk,
    missed = ComicTokens.TextSecondary,
    locked = ComicTokens.TextSecondary,
    prayedFill = ComicTokens.CoralRed,
    mark = ComicTokens.SolidBlack,
    ringStroke = 3.dp,
    heading = MaterialTheme.typography.titleLarge.copy(fontFamily = BangersFontFamily, letterSpacing = 0.6.sp),
    uppercaseHeadings = true,
    chipShape = RoundedCornerShape(10.dp),
    chipBorderWidth = 2.dp,
    chipBorder = ComicTokens.SolidBlack,
    chipFill = ComicTokens.PanelSurface,
    chipSelectedFill = ComicTokens.CoralRed,
    chipSelectedBorder = ComicTokens.SolidBlack,
    chipShadow = true,
    dotPrayed = ComicTokens.CoralRed,
    dotMissed = ComicTokens.TextSecondary.copy(alpha = 0.35f),
    dotOutline = ComicTokens.SolidBlack
)

/** The Light theme's green: 5:1 on white. */
private val PlainPrayedGreen = Color(0xFF15803D)

/**
 * Light, and Simple Mode in any theme: quiet and clear. The theme's own colours (green for
 * prayed, red for missed), soft rounded day tabs with a gentle tint for the chosen one, no capitals
 * or shadows. On a dark theme the next prayer keeps the dark panel, and pixel frames keep their room.
 */
@Composable
private fun plainNamazStyle(playful: Boolean = true): NamazStyle {
    val colors = PixelTheme.colors
    val pixelFrames = PixelTheme.mode == ThemeMode.Pixel || PixelTheme.mode == ThemeMode.System
    // Green for prayed on any theme: Comic's tertiary is a pale lavender, faint on white.
    val prayed = if (colors.isDark) colors.tertiary else PlainPrayedGreen
    return NamazStyle(
        heroPanel = if (colors.isDark) PixelPanelVariant.BLUE else PixelPanelVariant.BEIGE,
        cardPadding = if (pixelFrames) 20.dp else 16.dp,
        pending = colors.primaryText,
        prayed = prayed,
        missed = colors.error,
        locked = colors.onSurfaceVariant,
        prayedFill = prayed.copy(alpha = 0.15f),
        mark = null,
        ringStroke = 3.dp,
        heading = MaterialTheme.typography.titleMedium,
        uppercaseHeadings = false,
        chipShape = RoundedCornerShape(10.dp),
        chipBorderWidth = 1.dp,
        chipBorder = colors.onSurface.copy(alpha = 0.2f),
        chipFill = colors.surface,
        chipSelectedFill = colors.primary.copy(alpha = 0.12f),
        chipSelectedBorder = colors.primaryText,
        chipShadow = false,
        dotPrayed = prayed,
        dotMissed = colors.error.copy(alpha = 0.35f),
        dotOutline = colors.onSurfaceVariant,
        playful = playful
    )
}

/** A section heading in the theme's style. */
@Composable
internal fun NamazHeading(text: String, modifier: Modifier = Modifier) {
    val style = namazStyle()
    Text(
        text = if (style.uppercaseHeadings) text.uppercase() else text,
        style = style.heading,
        color = PixelTheme.colors.primaryText,
        modifier = modifier
    )
}
