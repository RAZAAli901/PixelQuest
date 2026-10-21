package com.pixelquest.app.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.chess.domain.BoardPosition
import com.pixelquest.app.chess.domain.squareName
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.Rubik

object ChessBoardTags {
    const val BOARD = "chess_board"
    fun square(name: String) = "chess_square_$name"
}

/** What a board shows besides its pieces (the design's highlights). */
data class BoardMarks(
    val lastMove: Set<String> = emptySet(),
    val selected: String? = null,
    val dots: Set<String> = emptySet(),
    val check: String? = null
)

/**
 * The board (J139), as the design draws it: 8 by 8 in the skin's light and dark squares, a
 * border and small shadow, the last move and the selected square in the highlight colour, a king
 * in check in red, legal-move dots, and coordinates (ranks on the left file, files on the bottom
 * rank). It draws any position from its FEN; [flipped] puts Black at the bottom, as a
 * player with Black sees it.
 */
@Composable
fun ChessBoard(
    fen: String,
    modifier: Modifier = Modifier,
    marks: BoardMarks = BoardMarks(),
    flipped: Boolean = false,
    animate: Boolean = false,
    square: @Composable (name: String, content: @Composable () -> Unit) -> Unit = { _, content -> content() }
) {
    val position = BoardPosition.of(fen)
    BoxWithConstraints(
        modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag(ChessBoardTags.BOARD)
            .comic(radius = PqDimens.RadiusSm, shadow = 2.dp)
    ) {
        val side = maxWidth / 8
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 8) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 8) {
                        val index = if (flipped) (7 - row) * 8 + (7 - col) else row * 8 + col
                        val name = squareName(index)
                        val light = (index / 8 + index % 8) % 2 == 0
                        val bg = when {
                            marks.check == name -> ChessLook.check
                            name in marks.lastMove || name == marks.selected -> ChessLook.highlight
                            light -> ChessLook.light
                            else -> ChessLook.dark
                        }
                        val shown = if (animate) androidx.compose.animation.animateColorAsState(bg, androidx.compose.animation.core.tween(220), label = "square").value else bg
                        Box(Modifier.size(side).background(shown).testTag(ChessBoardTags.square(name)), contentAlignment = Alignment.Center) {
                            square(name) {
                                position.pieces[index]?.let { PieceGlyph(it, size = side) }
                                if (name in marks.dots) Box(Modifier.size(12.dp).clip(CircleShape).background(ChessLook.dot))
                                Coordinates(name, showRank = col == 0, showFile = row == 7)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Coordinates(name: String, showRank: Boolean, showFile: Boolean) {
    val style = TextStyle(fontFamily = Rubik, fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 9.sp, color = PqColors.Ink2)
    Box(Modifier.fillMaxSize().padding(2.dp)) {
        if (showRank) Text(name.substring(1), style = style, modifier = Modifier.align(Alignment.TopStart))
        if (showFile) Text(name.substring(0, 1), style = style, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

/**
 * A piece as the design draws it (J140): the same solid glyph for both sides, Black's in ink and
 * White's in the skin's paper with an ink outline, so both read on light and dark squares.
 */
@Composable
fun PieceGlyph(piece: Char, size: androidx.compose.ui.unit.Dp) {
    val white = piece.isUpperCase()
    val style = TextStyle(fontSize = (size.value * 0.68f).sp, lineHeight = (size.value * 0.68f).sp)
    val glyph = ChessLook.glyph(piece)
    Box(contentAlignment = Alignment.Center) {
        if (white) {
            Text(glyph, style = style.copy(color = PqColors.Ink, drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = size.value * 0.09f, join = androidx.compose.ui.graphics.StrokeJoin.Round)))
        }
        Text(glyph, style = style.copy(color = if (white) PqColors.Paper else PqColors.Ink))
    }
}
