package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens

// A study party's look (docs/study-party.md, mocks 4H to 4J): the skin's chips, and medals on the board.

/** The app's Simple Mode, for a study party's screens: plain shapes, and no points, places or board. */
val LocalStudySimpleMode = staticCompositionLocalOf { false }

/** A chip's or tab's shape: the skin's pill (square in Pixel). */
@Composable
fun studyChipShape(): Shape = PqCircle

/** A chip's outline: the skin's thin ink. */
@Composable
fun studyBorderWidth(): Dp = PqDimens.BorderThin

/**
 * A chip's fill and outline in the skin's shape. It isn't clipped: drawing the shape is enough, and
 * a clip would keep taps from reaching it in tests (see QuranLook's framed()).
 */
@Composable
fun Modifier.studyChip(fill: Color): Modifier {
    val shape = studyChipShape()
    return this.background(fill, shape).border(studyBorderWidth(), PqColors.Ink, shape)
}

/** A medal on the board (mock 4J), behind a place's number. */
enum class Medal { GOLD, SILVER, BRONZE }

/** The medal for a place on the board: gold, silver and bronze for the first three with points. */
fun medalFor(place: Int, points: Int): Medal? = if (points <= 0) null else when (place) {
    1 -> Medal.GOLD
    2 -> Medal.SILVER
    3 -> Medal.BRONZE
    else -> null
}
