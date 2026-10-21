package com.pixelquest.app.darood.ui.race

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.RaceLine
import com.pixelquest.app.darood.domain.rankRace
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.chat.dayDate
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.PqTag
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** Why the board couldn't be shown. */
enum class RaceProblem { OFFLINE, FAILED }

/**
 * @param lines the board, in race order ([rankRace]); null until the first answer.
 * @param loading the board is being asked for.
 */
data class RaceBoardUiState(
    val lines: List<RaceLine>? = null,
    val loading: Boolean = false,
    val problem: RaceProblem? = null
)

/**
 * The race board (docs/darood-design.md, The milestone and the race): the parties whose leaders put
 * them on it, by finish then progress. It's read fresh from the server each time, never kept, since
 * it's other parties' standings.
 */
@HiltViewModel
class RaceBoardViewModel @Inject constructor(
    private val repository: DaroodPartyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaceBoardUiState())
    val uiState: StateFlow<RaceBoardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(loading = true, problem = null) }
        viewModelScope.launch {
            val result = repository.fetchRaceBoard()
            _uiState.update {
                when (result) {
                    is SupabaseResult.Success -> it.copy(loading = false, lines = rankRace(result.data))
                    is SupabaseResult.NetworkError -> it.copy(loading = false, problem = RaceProblem.OFFLINE)
                    else -> it.copy(loading = false, problem = RaceProblem.FAILED)
                }
            }
        }
    }
}

@Composable
fun RaceBoardScreen(onNavigateBack: () -> Unit, viewModel: RaceBoardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    RaceBoardContent(state = state, onNavigateBack = onNavigateBack, onRetry = viewModel::refresh)
}

@Composable
fun RaceBoardContent(state: RaceBoardUiState, onNavigateBack: () -> Unit, onRetry: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .testTag(DaroodTestTags.RACE_SCREEN)
    ) {
        PqTopBar(stringResource(R.string.darood_race_title), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = PqDimens.ScreenPad).padding(top = 8.dp))
        val lines = state.lines
        when {
            state.problem != null && lines == null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Text(
                    text = stringResource(if (state.problem == RaceProblem.OFFLINE) R.string.darood_race_offline else R.string.darood_race_failed),
                    style = PqType.Body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(DaroodTestTags.RACE_PROBLEM)
                )
                PqButton(text = stringResource(R.string.darood_race_retry), onClick = onRetry)
            }
            lines == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PqColors.Ink, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
            lines.isEmpty() -> Text(
                text = stringResource(R.string.darood_race_empty),
                style = PqType.Body,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp).testTag(DaroodTestTags.RACE_EMPTY)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(PqDimens.Gap),
                modifier = Modifier.fillMaxSize()
            ) {
                item { Text(text = stringResource(R.string.darood_race_intro), style = PqType.Meta) }
                itemsIndexed(lines) { index, line -> RaceRow(place = index + 1, line = line) }
            }
        }
    }
}

/**
 * One party in the race: its place (in its medal, for the first three to finish), name and size, and
 * how far it is (or when it finished). The player's own parties stand out in sky, with "Yours".
 */
@Composable
private fun RaceRow(place: Int, line: RaceLine) {
    val progressText = line.completedAt?.let {
        stringResource(R.string.darood_race_finished, dayDate(it.atZone(ZoneId.systemDefault()).toLocalDate()))
    } ?: stringResource(R.string.darood_race_progress, formatCount(line.total), formatCount(line.milestone), Math.round(line.progress * 100))
    val members = pluralStringResource(R.plurals.darood_members, line.memberCount, line.memberCount)
    val said = listOfNotNull(
        stringResource(R.string.darood_race_place, place),
        line.name,
        if (line.mine) stringResource(R.string.darood_race_yours) else null,
        members,
        progressText
    ).joinToString(", ")
    val medal = if (line.completedAt == null) null else when (place) {
        1 -> PqColors.Gold
        2 -> PqColors.Sunken
        3 -> PqColors.Amber
        else -> null
    }
    PqCard(
        bg = if (line.mine) PqColors.Sky else PqColors.Paper,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DaroodTestTags.raceRow(place, line.mine))
            .clearAndSetSemantics { contentDescription = said }
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(30.dp).comic(bg = medal ?: PqColors.Paper, radius = 15.dp, border = PqDimens.BorderThin, shadow = 0.dp)
                ) {
                    Text(text = place.toString(), style = PqType.Button)
                }
                Text(text = line.name, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (line.mine) PqTag(stringResource(R.string.darood_race_yours), color = PqColors.Ink)
            }
            PqProgressBar(progress = line.progress, modifier = Modifier.fillMaxWidth(), height = 10.dp)
            Text(text = "$members  ·  $progressText", style = PqType.Meta.copy(color = PqColors.Ink2))
        }
    }
}
