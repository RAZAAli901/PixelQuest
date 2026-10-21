package com.pixelquest.app.chess.rules

/**
 * A small chess rules library for the phone (J141), used only for hints: which moves a piece has
 * (the legal-move dots), whether a king is in check (its highlight), and whether a move promotes
 * (the picker). The server checks every move with chess.js and decides everything; nothing here
 * is ever trusted. Checked against standard perft counts (ChessRulesTest).
 *
 * Squares are 0..63 from a8 (0) to h1 (63), as the board draws them.
 */
data class Move(val from: Int, val to: Int, val promotion: Char? = null) {
    val uci: String get() = name(from) + name(to) + (promotion?.lowercaseChar()?.toString() ?: "")

    companion object {
        fun name(square: Int) = "${"abcdefgh"[square % 8]}${8 - square / 8}"
        fun index(name: String): Int? {
            if (name.length != 2) return null
            val f = name[0] - 'a'
            val r = name[1] - '1'
            return if (f in 0..7 && r in 0..7) (7 - r) * 8 + f else null
        }

        fun ofUci(uci: String): Move? {
            if (uci.length !in 4..5) return null
            val from = index(uci.substring(0, 2)) ?: return null
            val to = index(uci.substring(2, 4)) ?: return null
            return Move(from, to, uci.getOrNull(4))
        }
    }
}

class Position private constructor(
    private val board: CharArray,
    val whiteToMove: Boolean,
    /** "KQkq" or a part of it, "" for none. */
    val castling: String,
    val enPassant: Int?,
    val halfmove: Int,
    val fullmove: Int
) {
    fun pieceAt(square: Int): Char? = board[square].takeIf { it != '.' }

    private fun isWhite(c: Char) = c.isUpperCase()
    private fun mine(c: Char) = c != '.' && isWhite(c) == whiteToMove
    private fun file(s: Int) = s % 8
    private fun rank(s: Int) = 7 - s / 8

    /** Every legal move for the side to move. */
    fun legalMoves(): List<Move> = pseudoMoves().filter { !apply(it).kingAttacked(whiteToMove) }

    /** The legal moves of the piece on [from]. */
    fun movesFrom(from: Int): List<Move> = legalMoves().filter { it.from == from }

    /** Whether the side to move is in check. */
    fun inCheck(): Boolean = kingAttacked(whiteToMove)

    fun isCheckmate() = inCheck() && legalMoves().isEmpty()

    /** Whether a move from [from] to [to] promotes (so the picker shows). */
    fun promotes(from: Int, to: Int): Boolean {
        val p = board[from]
        return (p == 'P' && rank(to) == 7) || (p == 'p' && rank(to) == 0)
    }

    fun kingSquare(white: Boolean): Int? = board.indexOfFirst { it == if (white) 'K' else 'k' }.takeIf { it >= 0 }

    private fun kingAttacked(white: Boolean): Boolean {
        val king = kingSquare(white) ?: return false
        return attacked(king, byWhite = !white)
    }

    /** Whether [square] is attacked by the given side. */
    fun attacked(square: Int, byWhite: Boolean): Boolean {
        val f = file(square)
        val r = rank(square)
        fun at(df: Int, dr: Int): Char? {
            val nf = f + df
            val nr = r + dr
            return if (nf in 0..7 && nr in 0..7) board[(7 - nr) * 8 + nf] else null
        }
        fun theirs(c: Char?, kind: Char) = c != null && c != '.' && c.lowercaseChar() == kind && isWhite(c) == byWhite
        // Pawns: a white pawn attacks up the board.
        val pawnDir = if (byWhite) -1 else 1
        if (theirs(at(-1, pawnDir), 'p') || theirs(at(1, pawnDir), 'p')) return true
        for ((df, dr) in KNIGHT) if (theirs(at(df, dr), 'n')) return true
        for ((df, dr) in KING) if (theirs(at(df, dr), 'k')) return true
        for ((df, dr) in ROOK_DIRS) if (slides(f, r, df, dr) { theirs(it, 'r') || theirs(it, 'q') }) return true
        for ((df, dr) in BISHOP_DIRS) if (slides(f, r, df, dr) { theirs(it, 'b') || theirs(it, 'q') }) return true
        return false
    }

    private inline fun slides(f: Int, r: Int, df: Int, dr: Int, hit: (Char) -> Boolean): Boolean {
        var nf = f + df
        var nr = r + dr
        while (nf in 0..7 && nr in 0..7) {
            val c = board[(7 - nr) * 8 + nf]
            if (c != '.') return hit(c)
            nf += df
            nr += dr
        }
        return false
    }

    private fun sq(f: Int, r: Int) = (7 - r) * 8 + f

    private fun pseudoMoves(): List<Move> {
        val out = ArrayList<Move>(48)
        for (s in 0 until 64) {
            val p = board[s]
            if (!mine(p)) continue
            val f = file(s)
            val r = rank(s)
            fun add(nf: Int, nr: Int): Boolean {
                if (nf !in 0..7 || nr !in 0..7) return false
                val t = sq(nf, nr)
                val c = board[t]
                if (c != '.' && mine(c)) return false
                out += Move(s, t)
                return c == '.'
            }
            when (p.lowercaseChar()) {
                'p' -> {
                    val dir = if (whiteToMove) 1 else -1
                    val start = if (whiteToMove) 1 else 6
                    val last = if (whiteToMove) 7 else 0
                    fun pawn(t: Int) {
                        if (rank(t) == last) "qrbn".forEach { out += Move(s, t, it) } else out += Move(s, t)
                    }
                    val one = r + dir
                    if (one in 0..7 && board[sq(f, one)] == '.') {
                        pawn(sq(f, one))
                        if (r == start && board[sq(f, r + 2 * dir)] == '.') out += Move(s, sq(f, r + 2 * dir))
                    }
                    for (df in intArrayOf(-1, 1)) {
                        val nf = f + df
                        if (nf !in 0..7 || one !in 0..7) continue
                        val t = sq(nf, one)
                        val c = board[t]
                        if ((c != '.' && !mine(c)) || t == enPassant) pawn(t)
                    }
                }
                'n' -> for ((df, dr) in KNIGHT) add(f + df, r + dr)
                'k' -> {
                    for ((df, dr) in KING) add(f + df, r + dr)
                    castles(s, out)
                }
                'b' -> for ((df, dr) in BISHOP_DIRS) { var k = 1; while (add(f + df * k, r + dr * k)) k++ }
                'r' -> for ((df, dr) in ROOK_DIRS) { var k = 1; while (add(f + df * k, r + dr * k)) k++ }
                'q' -> for ((df, dr) in ROOK_DIRS + BISHOP_DIRS) { var k = 1; while (add(f + df * k, r + dr * k)) k++ }
            }
        }
        return out
    }

    private fun castles(king: Int, out: MutableList<Move>) {
        val white = whiteToMove
        val home = if (white) 60 else 4
        if (king != home || attacked(home, !white)) return
        val (short, long) = if (white) 'K' to 'Q' else 'k' to 'q'
        val rook = if (white) 'R' else 'r'
        if (short in castling && board[home + 1] == '.' && board[home + 2] == '.' && board[home + 3] == rook &&
            !attacked(home + 1, !white) && !attacked(home + 2, !white)
        ) out += Move(home, home + 2)
        if (long in castling && board[home - 1] == '.' && board[home - 2] == '.' && board[home - 3] == '.' && board[home - 4] == rook &&
            !attacked(home - 1, !white) && !attacked(home - 2, !white)
        ) out += Move(home, home - 2)
    }

    /** The position after [move] (not checked: legalMoves gives the legal ones). */
    fun apply(move: Move): Position {
        val b = board.copyOf()
        val p = b[move.from]
        val captured = b[move.to]
        b[move.to] = move.promotion?.let { if (whiteToMove) it.uppercaseChar() else it.lowercaseChar() } ?: p
        b[move.from] = '.'
        val kind = p.lowercaseChar()
        // En passant: the pawn taken stands beside, not on, the square moved to.
        if (kind == 'p' && move.to == enPassant && captured == '.') b[move.to + if (whiteToMove) 8 else -8] = '.'
        // Castling moves the rook too.
        if (kind == 'k' && kotlin.math.abs(move.to - move.from) == 2) {
            if (move.to > move.from) { b[move.from + 1] = b[move.from + 3]; b[move.from + 3] = '.' } else { b[move.from - 1] = b[move.from - 4]; b[move.from - 4] = '.' }
        }
        var rights = castling
        fun lose(square: Int) {
            rights = when (square) {
                60 -> rights.replace("K", "").replace("Q", "")
                4 -> rights.replace("k", "").replace("q", "")
                63 -> rights.replace("K", "")
                56 -> rights.replace("Q", "")
                7 -> rights.replace("k", "")
                0 -> rights.replace("q", "")
                else -> rights
            }
        }
        lose(move.from)
        lose(move.to)
        val ep = if (kind == 'p' && kotlin.math.abs(move.to - move.from) == 16) (move.from + move.to) / 2 else null
        return Position(b, !whiteToMove, rights, ep, if (kind == 'p' || captured != '.') 0 else halfmove + 1, if (whiteToMove) fullmove else fullmove + 1)
    }

    companion object {
        private val KNIGHT = listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
        private val KING = listOf(1 to 0, 1 to 1, 0 to 1, -1 to 1, -1 to 0, -1 to -1, 0 to -1, 1 to -1)
        private val ROOK_DIRS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        private val BISHOP_DIRS = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

        const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        /** The position a FEN describes, or null if it isn't one. */
        fun of(fen: String): Position? {
            val f = fen.trim().split(' ')
            val rows = f.getOrNull(0)?.split('/') ?: return null
            if (rows.size != 8) return null
            val b = CharArray(64) { '.' }
            var i = 0
            for (row in rows) {
                var w = 0
                for (c in row) {
                    if (c.isDigit()) { i += c.digitToInt(); w += c.digitToInt() } else if (c.lowercaseChar() in "kqrbnp") { if (i >= 64) return null; b[i++] = c; w++ } else return null
                }
                if (w != 8) return null
            }
            if (i != 64) return null
            return Position(
                b, f.getOrNull(1) != "b", f.getOrNull(2)?.takeIf { it != "-" }.orEmpty(),
                f.getOrNull(3)?.takeIf { it != "-" }?.let { Move.index(it) },
                f.getOrNull(4)?.toIntOrNull() ?: 0, f.getOrNull(5)?.toIntOrNull() ?: 1
            )
        }
    }
}
