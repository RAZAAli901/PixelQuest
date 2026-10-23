package com.pixelquest.app.chess.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.chess.data.ChessRepository
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.ui.components.PqConfirmDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The player's game in play when the app starts (J163): if the app was closed or killed mid-game,
 * it opens again, once. The game was the server's all along, clocks included.
 */
@HiltViewModel
class ChessResumeViewModel @Inject constructor(private val chess: ChessRepository) : ViewModel() {
    private val _game = MutableStateFlow<String?>(null)
    val game: StateFlow<String?> = _game

    init {
        checkLiveGame()
    }

    fun checkLiveGame() {
        viewModelScope.launch {
            (chess.myLiveGame() as? SupabaseResult.Success)?.data?.let {
                if (it.status.live) _game.value = it.id
            }
        }
    }

    /** Opened: not again this run. */
    fun opened() {
        _game.value = null
    }
}

/** The screen stays on while the game goes on (J163). */
@Composable
fun KeepScreenOn(on: Boolean) {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(on) {
        if (on) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/**
 * Back during a game asks first (J163): leaving isn't resigning, and the clock keeps running on
 * the server, so the game can be opened again from the party.
 */
@Composable
fun LeaveGameGuard(
    playing: Boolean,
    asking: Boolean = false,
    onAskingChange: (Boolean) -> Unit = {},
    onLeave: () -> Unit,
) {
    var localAsking by rememberSaveable { mutableStateOf(false) }
    val show = asking || localAsking
    BackHandler(enabled = playing) {
        localAsking = true
        onAskingChange(true)
    }
    if (show) {
        PqConfirmDialog(
            title = stringResource(R.string.chess_leave_title),
            message = stringResource(R.string.chess_leave_message),
            confirm = stringResource(R.string.chess_leave),
            onConfirm = {
                localAsking = false
                onAskingChange(false)
                onLeave()
            },
            onDismiss = {
                localAsking = false
                onAskingChange(false)
            },
        )
    }
}
