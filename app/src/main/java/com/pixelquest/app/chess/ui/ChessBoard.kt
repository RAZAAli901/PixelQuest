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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.sp
import com.pixelquest.app.chess.domain.BoardPosition
import com.pixelquest.app.chess.domain.squareName
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.motion.AnimatedPiece
import com.pixelquest.app.ui.motion.BoardDim
import com.pixelquest.app.ui.motion.CaptureEffect
import com.pixelquest.app.ui.motion.CheckTag
import com.pixelquest.app.ui.motion.MoveDot
import com.pixelquest.app.ui.motion.PromotionMorph
import com.pixelquest.app.ui.motion.ResultStamp
import com.pixelquest.app.ui.motion.checkPulse
import com.pixelquest.app.ui.motion.kingShake
import com.pixelquest.app.ui.motion.kingTopple
import com.pixelquest.app.ui.motion.pieceLift
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.Rubik

object ChessBoardTags {
    const val BOARD = "chess_board"
    fun square(name: String) = "chess_square_$name"
}

/**
 * How a board animates (the animations handoff, C01 to C04): [bullet] for 1-minute games' quicker
 * moves, [mine] the player's colour (white or not) for a haptic as their own piece lands, and
 * [mated] for a checkmate's topple, dim and stamp.
 */
data class BoardMotion(val bullet: Boolean = false, val mine: Boolean? = null, val mated: Boolean = false)

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
    motion: BoardMotion = BoardMotion(),
    /** TalkBack's tap on a square (J165): the same as a finger's. */
    onSquareClick: ((Int) -> Unit)? = null,
    square: @Composable (name: String, content: @Composable () -> Unit) -> Unit = { _, content -> content() }
) {
    val position = BoardPosition.of(fen)
    // Animated, the pieces are drawn on a layer of their own, each following its id (C01).
    val tracker = remember { PieceTracker() }
    val frame = remember(fen) { tracker.update(position.pieces) }
    var checks by remember { mutableIntStateOf(0) }
    LaunchedEffect(marks.check) { if (marks.check != null) checks++ }
    val dotOrder = marks.dots.sorted()
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
                            marks.check == name && !animate -> ChessLook.check
                            name in marks.lastMove || name == marks.selected -> ChessLook.highlight
                            light -> ChessLook.light
                            else -> ChessLook.dark
                        }
                        val shown = if (animate) androidx.compose.animation.animateColorAsState(bg, androidx.compose.animation.core.tween(220), label = "square").value else bg
                        val said = squareSaid(name, position.pieces[index], name in marks.dots)
                        Box(
                            Modifier
                                .size(side)
                                .background(shown)
                                .checkPulse(animate && marks.check == name)
                                .testTag(ChessBoardTags.square(name))
                                .clearAndSetSemantics {
                                    contentDescription = said
                                    if (name == marks.selected) selected = true
                                    onSquareClick?.let { tap -> onClick { tap(index); true } }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            square(name) {
                                if (!animate) position.pieces[index]?.let { PieceGlyph(it, size = side) }
                                when {
                                    name !in marks.dots -> Unit
                                    animate -> MoveDot(capture = position.pieces[index] != null, order = dotOrder.indexOf(name), color = ChessLook.dot)
                                    else -> Box(Modifier.size(12.dp).clip(CircleShape).background(ChessLook.dot))
                                }
                                Coordinates(name, showRank = col == 0, showFile = row == 7)
                            }
                        }
                    }
                }
            }
        }
        if (animate) {
            PieceLayer(frame, side, flipped, marks, motion, checks)
            if (motion.mated) {
                BoardDim(true)
                ResultStamp(stringResource(R.string.motion_checkmate), visible = true, modifier = Modifier.align(Alignment.Center))
            } else {
                CheckTag(if (marks.check != null) checks else 0, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
    }
}

/**
 * The pieces above the squares (C01 to C04, C09): each glides to its square, the selected one
 * lifts, a king in check shakes and a mated one topples, a promoted pawn morphs, and what was
 * captured bursts and flies off towards the side that took it. TalkBack reads the squares, not this.
 */
@Composable
private fun PieceLayer(frame: TrackedFrame, side: Dp, flipped: Boolean, marks: BoardMarks, motion: BoardMotion, checks: Int) {
    val density = LocalDensity.current
    fun colRow(square: Int) = if (flipped) (7 - square % 8) to (7 - square / 8) else (square % 8) to (square / 8)
    Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        for (piece in frame.pieces) {
            key(piece.id) {
                val (col, row) = colRow(piece.square)
                val name = squareName(piece.square)
                val king = piece.piece == 'K' || piece.piece == 'k'
                val effects = Modifier
                    .pieceLift(name == marks.selected)
                    .then(if (king && name == marks.check) Modifier.kingShake(checks) else Modifier)
                    .then(if (king && name == marks.check) Modifier.kingTopple(motion.mated) else Modifier)
                AnimatedPiece(col, row, side, bullet = motion.bullet, mine = motion.mine == piece.piece.isUpperCase()) {
                    Box(effects, contentAlignment = Alignment.Center) {
                        if (piece.promoted) {
                            PromotionMorph(frame.serial, side, pawn = { PieceGlyph(if (piece.piece.isUpperCase()) 'P' else 'p', size = side) }, promoted = { PieceGlyph(piece.piece, size = side) })
                        } else {
                            PieceGlyph(piece.piece, size = side)
                        }
                    }
                }
            }
        }
        for (gone in frame.captured) {
            key(frame.serial, gone.id) {
                val (col, row) = colRow(gone.square)
                // Towards the side that took it: White's tray is at the bottom unless the board is flipped.
                val toBottom = gone.piece.isLowerCase() != flipped
                val fly = with(density) { if (toBottom) (side * (8 - row) + 30.dp).toPx() else -(side * (row + 1) + 30.dp).toPx() }
                CaptureEffect(col, row, side, Offset(0f, fly)) { PieceGlyph(gone.piece, size = side) }
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
