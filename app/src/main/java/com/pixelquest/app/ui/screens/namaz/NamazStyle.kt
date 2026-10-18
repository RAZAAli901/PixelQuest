package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelPanelVariant
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
    /** Game touches (🔥, and XP from Phase 6); off in Simple Mode. */
    val playful: Boolean = true
)

/** The current theme's Namaz screen. */
@Composable
fun namazStyle(): NamazStyle = when (PixelTheme.mode) {
    ThemeMode.Pixel, ThemeMode.System -> pixelNamazStyle()
    else -> themeNamazStyle()
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

/** The theme's own colours, for themes not yet given their own Namaz look. */
@Composable
private fun themeNamazStyle(): NamazStyle {
    val colors = PixelTheme.colors
    return NamazStyle(
        heroPanel = PixelPanelVariant.BEIGE,
        cardPadding = 16.dp,
        pending = colors.primary,
        prayed = colors.tertiary,
        missed = colors.error,
        locked = colors.onSurfaceVariant,
        prayedFill = colors.tertiary.copy(alpha = 0.25f),
        mark = null,
        ringStroke = 3.dp,
        heading = MaterialTheme.typography.titleMedium,
        uppercaseHeadings = false,
        chipShape = RoundedCornerShape(8.dp),
        chipBorderWidth = 1.dp,
        chipBorder = colors.pixelBorder,
        chipFill = colors.surface,
        chipSelectedFill = colors.primary.copy(alpha = 0.2f),
        chipSelectedBorder = colors.primaryText,
        chipShadow = false,
        dotPrayed = colors.tertiary,
        dotMissed = colors.error,
        dotOutline = colors.onSurfaceVariant
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
