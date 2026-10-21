package com.pixelquest.app.chess.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The player's chess choices on this phone (J160, J161). The design has one board and one set of
 * pieces, so there's no style to pick.
 */
data class ChessPrefs(
    /** Ask before each move is sent. */
    val confirmMoves: Boolean = false,
    /** Animate the last move's squares. */
    val animate: Boolean = true,
    /** A pawn on the last rank becomes a queen without asking. */
    val autoQueen: Boolean = false,
    /** Sounds and haptics for moves, captures, checks and the end (J161). */
    val sounds: Boolean = true
)

interface ChessSettings {
    val prefs: StateFlow<ChessPrefs>
    fun update(change: (ChessPrefs) -> ChessPrefs)
}

@Singleton
class PrefsChessSettings @Inject constructor(@ApplicationContext context: Context) : ChessSettings {
    private val store = context.getSharedPreferences("pixelquest_chess", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        ChessPrefs(
            confirmMoves = store.getBoolean(CONFIRM, false),
            animate = store.getBoolean(ANIMATE, true),
            autoQueen = store.getBoolean(AUTO_QUEEN, false),
            sounds = store.getBoolean(SOUNDS, true)
        )
    )
    override val prefs: StateFlow<ChessPrefs> = state.asStateFlow()

    @Synchronized
    override fun update(change: (ChessPrefs) -> ChessPrefs) {
        val next = change(state.value)
        store.edit()
            .putBoolean(CONFIRM, next.confirmMoves)
            .putBoolean(ANIMATE, next.animate)
            .putBoolean(AUTO_QUEEN, next.autoQueen)
            .putBoolean(SOUNDS, next.sounds)
            .apply()
        state.value = next
    }

    private companion object {
        const val CONFIRM = "confirm_moves"
        const val ANIMATE = "animate"
        const val AUTO_QUEEN = "auto_queen"
        const val SOUNDS = "sounds"
    }
}

/** For tests and previews. */
class MemoryChessSettings(initial: ChessPrefs = ChessPrefs()) : ChessSettings {
    private val state = MutableStateFlow(initial)
    override val prefs: StateFlow<ChessPrefs> = state
    override fun update(change: (ChessPrefs) -> ChessPrefs) { state.value = change(state.value) }
}
