package com.pixelquest.app.darood.ui.race

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
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
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
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
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_race_title),
                navigationIcon = {
                    com.pixelquest.app.ui.components.PqBackButton(onNavigateBack)
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).testTag(DaroodTestTags.RACE_SCREEN)) {
            val lines = state.lines
            when {
                state.problem != null && lines == null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    modifier = Modifier.fillMaxSize().padding(24.dp)
                ) {
                    Text(
                        text = stringResource(if (state.problem == RaceProblem.OFFLINE) R.string.darood_race_offline else R.string.darood_race_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(DaroodTestTags.RACE_PROBLEM)
                    )
                    PixelButton(text = stringResource(R.string.darood_race_retry), onClick = onRetry)
                }
                lines == null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) { CircularProgressIndicator(color = colors.primary) }
                lines.isEmpty() -> Text(
                    text = stringResource(R.string.darood_race_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp).testTag(DaroodTestTags.RACE_EMPTY)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.darood_race_intro),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    itemsIndexed(lines) { index, line -> RaceRow(place = index + 1, line = line) }
                }
            }
        }
    }
}

/** One party in the race: its place, name and size, and how far it is (or when it finished). */
@Composable
private fun RaceRow(place: Int, line: RaceLine) {
    val colors = PixelTheme.colors
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
    PixelCard(
        // The player's own parties stand out: the board's accent panel, and "Yours".
        variant = if (line.mine) PixelPanelVariant.BEIGE else PixelPanelVariant.BORDER,
        contentPadding = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DaroodTestTags.raceRow(place, line.mine))
            .clearAndSetSemantics { contentDescription = said }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (line.completedAt != null && place <= 3) listOf("🥇", "🥈", "🥉")[place - 1] else "$place",
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnPanel(colors.primaryText),
                    modifier = Modifier.width(36.dp)
                )
                Text(
                    text = line.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (line.mine) FontWeight.Bold else FontWeight.Normal),
                    color = inkOnPanel(colors.primaryText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (line.mine) {
                    Text(text = stringResource(R.string.darood_race_yours), style = MaterialTheme.typography.labelMedium, color = inkOnPanel(colors.primaryText))
                }
            }
            PixelProgressBar(progress = line.progress, height = 10.dp)
            Text(text = "$members  ·  $progressText", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
        }
    }
}
