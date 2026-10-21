package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

// A study party's look in each theme (docs/study-party.md): chamfered 8-bit corners in Pixel, round
// and bold in Comic, plainly rounded in Light; medals on the board in all of them.

/** A chip's or tab's shape: the theme's own. */
@Composable
fun studyChipShape(): Shape = when (PixelTheme.mode) {
    ThemeMode.Pixel, ThemeMode.System -> CutCornerShape(4.dp)
    ThemeMode.Comic -> RoundedCornerShape(14.dp)
    else -> RoundedCornerShape(8.dp)
}

/** A chip's outline: Comic's is bold. */
@Composable
fun studyBorderWidth(): Dp = if (PixelTheme.mode == ThemeMode.Comic) 2.dp else 1.dp

/**
 * A chip's fill and outline in the theme's shape. It isn't clipped: drawing the shape is enough, and
 * a clip would keep taps from reaching it in tests (see QuranLook's framed()).
 */
@Composable
fun Modifier.studyChip(fill: Color): Modifier {
    val shape = studyChipShape()
    return this.background(fill, shape).border(studyBorderWidth(), PixelTheme.colors.pixelBorder, shape)
}

/**
 * The comic panel for a card that stands out (an item done, the player's own line): paper, so its
 * ink stays easy to read; elsewhere the card's own.
 */
val STANDOUT_COMIC: ComicPanelVariant = ComicPanelVariant.PAPER

/** The medal for a place on the board: gold, silver and bronze for the first three with points. */
fun medalFor(place: Int, points: Int): String? = if (points <= 0) null else when (place) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> null
}
