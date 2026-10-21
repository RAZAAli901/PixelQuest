package com.pixelquest.app.chess.domain

/**
 * A position's pieces from its FEN (Forsyth-Edwards Notation), for drawing: [pieces] holds the 64
 * squares from a8 to h1 (row by row, as FEN writes them), each a piece letter (upper case White,
 * lower case Black) or null. A FEN the app can't read is an empty board, never a crash.
 */
data class BoardPosition(val pieces: List<Char?>, val whiteToMove: Boolean) {
    operator fun get(square: String): Char? = squareIndex(square)?.let { pieces[it] }

    /** The square [colour]'s king stands on, if there is one. */
    fun kingOf(white: Boolean): String? = pieces.indexOfFirst { it == if (white) 'K' else 'k' }.takeIf { it >= 0 }?.let(::squareName)

    companion object {
        val EMPTY = BoardPosition(List(64) { null }, whiteToMove = true)

        fun of(fen: String): BoardPosition {
            val fields = fen.trim().split(' ')
            val rows = fields.firstOrNull()?.split('/') ?: return EMPTY
            if (rows.size != 8) return EMPTY
            val squares = ArrayList<Char?>(64)
            for (row in rows) {
                var width = 0
                for (c in row) {
                    when {
                        c.isDigit() -> {
                            repeat(c.digitToInt()) { squares += null }
                            width += c.digitToInt()
                        }
                        c.lowercaseChar() in "kqrbnp" -> { squares += c; width++ }
                        else -> return EMPTY
                    }
                }
                if (width != 8) return EMPTY
            }
            return BoardPosition(squares, whiteToMove = fields.getOrNull(1) != "b")
        }
    }
}

/** "e4" for index 36 (a8 is 0, h1 is 63). */
fun squareName(index: Int): String = "${"abcdefgh"[index % 8]}${8 - index / 8}"

/** The index of "e4" (36), or null for anything that isn't a square. */
fun squareIndex(square: String): Int? {
    if (square.length != 2) return null
    val file = square[0] - 'a'
    val rank = square[1] - '1'
    if (file !in 0..7 || rank !in 0..7) return null
    return (7 - rank) * 8 + file
}

/**
 * What each side has taken, worked out from the pieces left (J146): [byWhite] are Black's pieces
 * gone from the board, most valuable first, and [advantage] White's material lead (pawn 1, knight
 * and bishop 3, rook 5, queen 9; negative when Black leads). A promoted pawn counts as its new piece.
 */
data class Material(val byWhite: List<Char>, val byBlack: List<Char>, val advantage: Int) {
    companion object {
        private val START = mapOf('q' to 1, 'r' to 2, 'b' to 2, 'n' to 2, 'p' to 8)
        private val VALUE = mapOf('q' to 9, 'r' to 5, 'b' to 3, 'n' to 3, 'p' to 1)

        fun of(position: BoardPosition): Material {
            fun missing(white: Boolean): List<Char> = START.flatMap { (kind, count) ->
                val have = position.pieces.count { it != null && it.lowercaseChar() == kind && it.isUpperCase() == white }
                List((count - have).coerceAtLeast(0)) { if (white) kind.uppercaseChar() else kind }
            }
            fun worth(white: Boolean) = position.pieces.filterNotNull().filter { it.isUpperCase() == white }.sumOf { VALUE[it.lowercaseChar()] ?: 0 }
            return Material(byWhite = missing(white = false), byBlack = missing(white = true), advantage = worth(true) - worth(false))
        }
    }
}
