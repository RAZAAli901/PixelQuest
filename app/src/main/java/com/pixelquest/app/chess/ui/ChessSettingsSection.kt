package com.pixelquest.app.chess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.pixelquest.app.R
import com.pixelquest.app.chess.data.ChessPrefs
import com.pixelquest.app.chess.data.ChessSettings
import com.pixelquest.app.study.ui.ToggleRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChessSettingsViewModel @Inject constructor(private val settings: ChessSettings) : ViewModel() {
    val prefs = settings.prefs
    fun update(change: (ChessPrefs) -> ChessPrefs) = settings.update(change)
}

object ChessSettingsTags {
    const val CONFIRM = "chess_settings_confirm"
    const val ANIMATE = "chess_settings_animate"
    const val AUTO_QUEEN = "chess_settings_auto_queen"
    const val SOUNDS = "chess_settings_sounds"
}

@Composable
fun ChessSettingsRoute(viewModel: ChessSettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsState()
    ChessSettingsSection(prefs, viewModel::update)
}

/** Chess's settings in the app's Settings (J160): confirm moves, animation, auto-queen, sounds. */
@Composable
fun ChessSettingsSection(prefs: ChessPrefs, onChange: ((ChessPrefs) -> ChessPrefs) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier) {
        ToggleRow(stringResource(R.string.chess_settings_confirm), stringResource(R.string.chess_settings_confirm_body), prefs.confirmMoves, { on -> onChange { it.copy(confirmMoves = on) } }, ChessSettingsTags.CONFIRM)
        ToggleRow(stringResource(R.string.chess_settings_animate), stringResource(R.string.chess_settings_animate_body), prefs.animate, { on -> onChange { it.copy(animate = on) } }, ChessSettingsTags.ANIMATE)
        ToggleRow(stringResource(R.string.chess_settings_queen), stringResource(R.string.chess_settings_queen_body), prefs.autoQueen, { on -> onChange { it.copy(autoQueen = on) } }, ChessSettingsTags.AUTO_QUEEN)
        ToggleRow(stringResource(R.string.chess_settings_sounds), stringResource(R.string.chess_settings_sounds_body), prefs.sounds, { on -> onChange { it.copy(sounds = on) } }, ChessSettingsTags.SOUNDS)
    }
}
