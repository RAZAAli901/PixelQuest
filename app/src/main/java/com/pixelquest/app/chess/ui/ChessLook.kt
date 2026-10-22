package com.pixelquest.app.chess.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqSkinState
import com.pixelquest.app.ui.theme.PqType

/**
 * The owner's chess design (chess-ui/PQChess.dc.html) in the app's theme: its board, highlight
 * and clock tokens for each skin, so the screens draw exactly as designed. Everything else (ink,
 * paper, borders, type) is the skin's own PqColors and PqType, as the design uses them.
 */
object ChessLook {
    /** Light squares: the skin's paper. */
    val light: Color get() = PqColors.Paper

    /** Dark squares: Comic's sky (#8ecae6), Pixel's amber (#e8a33d). */
    val dark: Color get() = PqSkinState.spec.chessDark

    /** The last move and the selected square: #ffd95a / #fde7a0. */
    val highlight: Color get() = PqSkinState.spec.chessHighlight

    /** A king in check: the skin's primary colour. */
    val check: Color get() = PqColors.Red

    /** A legal-move dot: ink at 35%. */
    val dot: Color get() = Color(0x59161412)

    /** The clocks: 30 Bangers in Comic, 15 Press Start 2P in Pixel. */
    val clock: TextStyle get() = PqType.number(30)

    /** The pieces, as the design draws them: solid glyphs for both sides, without emoji style. */
    fun glyph(piece: Char): String = when (piece.lowercaseChar()) {
        'k' -> "\u265A"
        'q' -> "\u265B"
        'r' -> "\u265C"
        'b' -> "\u265D"
        'n' -> "\u265E"
        'p' -> "\u265F"
        else -> ""
    } + "\uFE0E"

    /** The design's player avatar colours, in turn: amber, lilac, sky. */
    fun avatar(index: Int): Color = when (index % 3) {
        0 -> PqColors.Amber
        1 -> PqColors.Lilac
        else -> PqColors.Sky
    }
}
