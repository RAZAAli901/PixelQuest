package com.pixelquest.app.chess.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.rules.Move
import com.pixelquest.app.chess.rules.Position
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import kotlin.math.roundToInt

/**
 * The board to play on (J142): tap a piece of yours to see its legal moves as dots, then tap
 * where it goes; or drag it there. The last move is highlighted, and a king in check is red. The
 * hints come from the phone's rules (ChessRules); [onMove] sends the move (UCI, with the piece a
 * pawn becomes from the promotion picker, J143), and the server decides.
 * [enabled] is false while it isn't the player's turn, or a move is on its way.
 */
@Composable
fun PlayableBoard(
    fen: String,
    myColour: ChessColour?,
    enabled: Boolean,
    lastMove: String?,
    onMove: (uci: String) -> Unit,
    modifier: Modifier = Modifier,
    flipped: Boolean = myColour == ChessColour.BLACK,
    /** Promote to a queen without asking (a chess setting). */
    autoQueen: Boolean = false,
    /** Animate the pieces and highlights (a chess setting; the animations handoff). */
    animate: Boolean = true,
    /** A 1-minute game: moves animate faster (C01). */
    bullet: Boolean = false,
) {
    val position = remember(fen) { Position.of(fen) }
    var selected by remember(fen) { mutableStateOf<Int?>(null) }
    var dragFrom by remember(fen) { mutableStateOf<Int?>(null) }
    var dragAt by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var promoting by remember(fen) { mutableStateOf<Pair<Int, Int>?>(null) }

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
        when {
            position?.promotes(from, to) != true -> onMove(Move(from, to).uci)
            autoQueen -> onMove(Move(from, to, 'q').uci)
            else -> promoting = from to to
        }
        return true
    }

    fun tap(square: Int) {
        val from = selected
        when {
            from != null && tryMove(from, square) -> Unit
            isMine(square) -> selected = if (selected == square) null else square
            else -> selected = null
        }
    }

    val check = position?.takeIf { it.inCheck() }?.kingSquare(position.whiteToMove)
    val mated = isMate(position, check)
    val marks = BoardMarks(
        lastMove = lastMove?.let { Move.ofUci(it) }?.let { setOf(Move.name(it.from), Move.name(it.to)) }.orEmpty(),
        selected = (dragFrom ?: selected)?.let(Move::name),
        dots = targets.map(Move::name).toSet(),
        check = check?.let(Move::name),
    )

    Box(modifier) {
        ChessBoard(
            fen = fen,
            marks = marks,
            flipped = flipped,
            animate = animate,
            motion = BoardMotion(bullet = bullet, mine = myColour?.let { it == ChessColour.WHITE }, mated = mated),
            onSquareClick = if (enabled) ({ square -> tap(square) }) else null,
            modifier = Modifier
                .onSizeChanged { size = it }
                .pointerInput(fen, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> squareAt(offset)?.let(::tap) }
                }
                .pointerInput(fen, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            squareAt(offset)?.takeIf(::isMine)?.let {
                                dragFrom = it
                                dragAt = offset
                            }
                        },
                        onDrag = { change, amount ->
                            if (dragFrom != null) {
                                change.consume()
                                dragAt += amount
                            }
                        },
                        onDragEnd = {
                            val from = dragFrom
                            dragFrom = null
                            val to = squareAt(dragAt)
                            if (from != null && to != null && !tryMove(from, to)) selected = from
                        },
                        onDragCancel = { dragFrom = null },
                    )
                },
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
        promoting?.let { (from, to) ->
            PromotionPicker(
                white = myColour == ChessColour.WHITE,
                onPick = {
                    promoting = null
                    onMove(Move(from, to, it).uci)
                },
                onCancel = { promoting = null },
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
            )
        }
    }
}

object PromotionTags {
    const val PICKER = "chess_promotion"
    fun piece(kind: Char) = "chess_promotion_$kind"
}

/** Which piece a pawn becomes (J143): queen, rook, bishop or knight, in the player's colour. */
@Composable
fun PromotionPicker(white: Boolean, onPick: (Char) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val names = mapOf('q' to com.pixelquest.app.R.string.chess_piece_queen, 'r' to com.pixelquest.app.R.string.chess_piece_rook, 'b' to com.pixelquest.app.R.string.chess_piece_bishop, 'n' to com.pixelquest.app.R.string.chess_piece_knight)
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        modifier = modifier
            .testTag(PromotionTags.PICKER)
            .then(Modifier.comic(radius = PqDimens.Radius, shadow = PqDimens.Shadow))
            .padding(12.dp),
    ) {
        androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.chess_promote_title), style = PqType.BodyStrong)
        androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            for ((kind, name) in names) {
                val label = androidx.compose.ui.res.stringResource(name)
                Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .comic(bg = ChessLook.light, radius = PqDimens.RadiusSm, border = PqDimens.BorderThin, shadow = 0.dp)
                        .clickable(onClickLabel = label, role = androidx.compose.ui.semantics.Role.Button) { onPick(kind) }
                        .semantics { contentDescription = label }
                        .testTag(PromotionTags.piece(kind)),
                ) { PieceGlyph(if (white) kind.uppercaseChar() else kind, size = 52.dp) }
            }
        }
        androidx.compose.material3.Text(
            androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.chess_promote_cancel),
            style = PqType.MetaStrong.copy(color = PqColors.RedText),
            modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onCancel).padding(8.dp),
        )
    }
}

/** Checkmate: in check, with no legal move (the board's topple and stamp, C04). */
private fun isMate(position: Position?, check: Int?): Boolean = check != null && position?.legalMoves()?.isEmpty() == true
