package com.pixelquest.app.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.chess.data.ChessRatingRepository
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HeadToHeadState(val name: String = "", val records: List<HeadToHead> = emptyList(), val loaded: Boolean = false, val offline: Boolean = false) {
    val total: HeadToHead? get() = records.firstOrNull { it.timeControl == null }
}

@HiltViewModel
class HeadToHeadViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val ratings: ChessRatingRepository) : ViewModel() {
    private val other: String = savedStateHandle[ARG_USER] ?: ""
    private val _state = MutableStateFlow(HeadToHeadState(name = savedStateHandle[ARG_NAME] ?: ""))
    val state: StateFlow<HeadToHeadState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = when (val read = ratings.headToHead(other)) {
                is SupabaseResult.Success -> _state.value.copy(records = read.data, loaded = true)
                else -> _state.value.copy(loaded = true, offline = true)
            }
        }
    }

    companion object {
        const val ARG_USER = "userId"
        const val ARG_NAME = "name"
    }
}

object HeadToHeadTags {
    const val SCREEN = "chess_h2h"
    const val TOTAL = "chess_h2h_total"
    const val BAR = "chess_h2h_bar"
    fun timeControl(tc: TimeControl) = "chess_h2h_tc_${tc.seconds}"
}

@Composable
fun HeadToHeadRoute(onNavigateBack: () -> Unit, viewModel: HeadToHeadViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    HeadToHeadScreen(state, onNavigateBack)
}

/**
 * Two players' rivalry (J157), as the design's: "You 7 – 5 Ali with 2 draws", a bar split by
 * wins, draws and losses, then the record in each time control.
 */
@Composable
fun HeadToHeadScreen(state: HeadToHeadState, onNavigateBack: () -> Unit) {
    val name = state.name.ifBlank { stringResource(R.string.chess_record_them) }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().background(PqColors.Ground).testTag(HeadToHeadTags.SCREEN).verticalScroll(rememberScrollState())
    ) {
        PqTopBar(stringResource(R.string.chess_h2h_title), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = PqDimens.ScreenPad).padding(top = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = PqDimens.ScreenPad)) {
            val total = state.total
            when {
                !state.loaded -> Unit
                total == null || total.games == 0 -> Text(stringResource(if (state.offline) R.string.chess_problem_offline else R.string.chess_h2h_none, name), style = PqType.Body)
                else -> {
                    Text(
                        text = pluralStringResource(R.plurals.chess_h2h_line, total.draws, total.myWins, total.theirWins, name, total.draws),
                        style = PqType.CardTitle,
                        modifier = Modifier.semantics { heading() }.testTag(HeadToHeadTags.TOTAL)
                    )
                    SplitBar(total, name)
                    PqFieldLabel(stringResource(R.string.chess_h2h_by_time))
                    Column(Modifier.fillMaxWidth().comic(radius = PqDimens.Radius, shadow = PqDimens.Shadow)) {
                        TimeControl.entries.forEach { tc ->
                            val record = state.records.firstOrNull { it.timeControl == tc }
                            Row(Modifier.fillMaxWidth().testTag(HeadToHeadTags.timeControl(tc)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(stringResource(R.string.chess_minutes, tc.seconds / 60), style = PqType.BodyStrong, modifier = Modifier.weight(1f))
                                Text(
                                    text = record?.let { stringResource(R.string.chess_h2h_cell, it.myWins, it.draws, it.theirWins) } ?: stringResource(R.string.chess_h2h_cell, 0, 0, 0),
                                    style = PqType.MetaStrong
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Wins, draws and losses as one bar, each part as wide as its share. */
@Composable
private fun SplitBar(record: HeadToHead, name: String) {
    val said = pluralStringResource(R.plurals.chess_h2h_line, record.draws, record.myWins, record.theirWins, name, record.draws)
    Row(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
            .testTag(HeadToHeadTags.BAR)
            .semantics { contentDescription = said }
            .comic(radius = PqDimens.RadiusSm, border = PqDimens.BorderThin, shadow = 0.dp)
    ) {
        listOf(record.myWins to PqColors.Gold, record.draws to PqColors.Paper, record.theirWins to PqColors.Ink).forEach { (count, colour) ->
            if (count > 0) Box(Modifier.weight(count.toFloat()).height(18.dp).background(colour))
        }
    }
}
