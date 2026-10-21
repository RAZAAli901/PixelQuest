package com.pixelquest.app.chess.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.rules.Move
import com.pixelquest.app.chess.rules.Position
import kotlin.math.roundToInt

/**
 * The board to play on (J142): tap a piece of yours to see its legal moves as dots, then tap
 * where it goes; or drag it there. The last move is highlighted, and a king in check is red. The
 * hints come from the phone's rules (ChessRules); [onMove] sends the move, and the server decides.
 * [enabled] is false while it isn't the player's turn, or a move is on its way.
 */
@Composable
fun PlayableBoard(
    fen: String,
    myColour: ChessColour?,
    enabled: Boolean,
    lastMove: String?,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    flipped: Boolean = myColour == ChessColour.BLACK
) {
    val position = remember(fen) { Position.of(fen) }
    var selected by remember(fen) { mutableStateOf<Int?>(null) }
    var dragFrom by remember(fen) { mutableStateOf<Int?>(null) }
    var dragAt by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun squareAt(offset: Offset): Int? {
        if (size.width == 0) return null
        val col = (offset.x / (size.width / 8f)).toInt()
        val row = (offset.y / (size.height / 8f)).toInt()
        if (col !in 0..7 || row !in 0..7) return null
        return if (flipped) (7 - row) * 8 + (7 - col) else row * 8 + col
    }
    fun isMine(square: Int): Boolean {
        val piece = position?.pieceAt(square) ?: return false
        return myColour != null && piece.isUpperCase() == (myColour == ChessColour.WHITE)
    }
    val targets = (dragFrom ?: selected)?.let { from -> position?.movesFrom(from)?.map { it.to }?.toSet() }.orEmpty()
    fun tryMove(from: Int, to: Int): Boolean {
        if (to !in (position?.movesFrom(from)?.map { it.to } ?: emptyList())) return false
        selected = null
        onMove(from, to)
        return true
    }

    val check = position?.takeIf { it.inCheck() }?.kingSquare(position.whiteToMove)
    val marks = BoardMarks(
        lastMove = lastMove?.let { Move.ofUci(it) }?.let { setOf(Move.name(it.from), Move.name(it.to)) }.orEmpty(),
        selected = (dragFrom ?: selected)?.let(Move::name),
        dots = targets.map(Move::name).toSet(),
        check = check?.let(Move::name)
    )

    Box(modifier) {
        ChessBoard(
            fen = fen,
            marks = marks,
            flipped = flipped,
            modifier = Modifier
                .onSizeChanged { size = it }
                .pointerInput(fen, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val square = squareAt(offset) ?: return@detectTapGestures
                        val from = selected
                        when {
                            from != null && tryMove(from, square) -> Unit
                            isMine(square) -> selected = if (selected == square) null else square
                            else -> selected = null
                        }
                    }
                }
                .pointerInput(fen, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            squareAt(offset)?.takeIf(::isMine)?.let { dragFrom = it; dragAt = offset }
                        },
                        onDrag = { change, amount -> if (dragFrom != null) { change.consume(); dragAt += amount } },
                        onDragEnd = {
                            val from = dragFrom
                            dragFrom = null
                            val to = squareAt(dragAt)
                            if (from != null && to != null && !tryMove(from, to)) selected = from
                        },
                        onDragCancel = { dragFrom = null }
                    )
                }
        )
        // The piece under the finger while it's dragged.
        val dragging = dragFrom
        val piece = dragging?.let { position?.pieceAt(it) }
        if (piece != null && size.width > 0) {
            val side = with(LocalDensity.current) { (size.width / 8f).toDp() }
            val half = size.width / 16f
            Box(Modifier.offset { IntOffset((dragAt.x - half).roundToInt(), (dragAt.y - half * 1.6f).roundToInt()) }) {
                PieceGlyph(piece, size = side * 1.2f)
            }
        }
    }
}
