package com.pixelquest.app.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.chess.data.ChessChallengeRepository
import com.pixelquest.app.chess.data.ChessRatingRepository
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.isAnswerable
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
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
import java.time.Instant
import javax.inject.Inject

data class IncomingState(
    val challenge: Challenge? = null,
    val fromName: String? = null,
    val record: HeadToHead? = null,
    val answering: Boolean = false,
    val problem: String? = null,
    val gone: Boolean = false,
    /** The game the accept made, to open. */
    val gameId: String? = null,
    val declined: Boolean = false
)

/** A challenge to the player (J151), loaded by its id, answered once. */
@HiltViewModel
class IncomingChallengeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val challenges: ChessChallengeRepository,
    private val ratings: ChessRatingRepository,
    private val party: PartyRepository
) : ViewModel() {
    private val id: String = savedStateHandle[ARG] ?: ""
    private val _state = MutableStateFlow(IncomingState())
    val state: StateFlow<IncomingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val challenge = (challenges.fetchChallenge(id) as? SupabaseResult.Success)?.data
            if (challenge == null) {
                _state.update { it.copy(gone = true) }
                return@launch
            }
            _state.update { it.copy(challenge = challenge) }
            launch {
                party.observeMembers(challenge.partyId).collect { members ->
                    _state.update { s -> s.copy(fromName = members.firstOrNull { it.userId == challenge.fromUser }?.username) }
                }
            }
            (ratings.headToHead(challenge.fromUser) as? SupabaseResult.Success)?.data?.firstOrNull { it.timeControl == null }?.let { r ->
                _state.update { it.copy(record = r) }
            }
        }
    }

    fun answer(accept: Boolean) {
        if (_state.value.answering || _state.value.challenge == null) return
        _state.update { it.copy(answering = true, problem = null) }
        viewModelScope.launch {
            when (val answer = challenges.respond(id, accept)) {
                is SupabaseResult.Success -> _state.update { it.copy(answering = false, gameId = answer.data?.id, declined = !accept) }
                is SupabaseResult.ServerError -> _state.update { it.copy(answering = false, problem = answer.message) }
                else -> _state.update { it.copy(answering = false, problem = ChessSessionViewModel.OFFLINE) }
            }
        }
    }

    companion object {
        const val ARG = "challengeId"
    }
}

object IncomingTags {
    const val SCREEN = "chess_incoming"
    const val COUNTDOWN = "chess_incoming_countdown"
    const val ACCEPT = "chess_incoming_accept"
    const val DECLINE = "chess_incoming_decline"
    const val GONE = "chess_incoming_gone"
}

@Composable
fun IncomingChallengeRoute(onNavigateBack: () -> Unit, onOpenGame: (String) -> Unit, viewModel: IncomingChallengeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.gameId, state.declined) {
        state.gameId?.let(onOpenGame)
        if (state.declined) onNavigateBack()
    }
    IncomingChallengeScreen(state, onAnswer = viewModel::answer, onNavigateBack = onNavigateBack)
}

/**
 * A challenge, full screen (J151), as the design's incoming card: who, the game (time, rated,
 * the colour you'd play), the record between you, and how long is left before it expires,
 * counting down. Accept or Decline; past its time it can only be closed.
 */
@Composable
fun IncomingChallengeScreen(state: IncomingState, onAnswer: (Boolean) -> Unit, onNavigateBack: () -> Unit, clock: () -> Instant = Instant::now) {
    Column(Modifier.fillMaxSize().background(PqColors.Ground).testTag(IncomingTags.SCREEN)) {
        PqTopBar(stringResource(R.string.chess_incoming_title), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = PqDimens.ScreenPad).padding(top = 8.dp))
        val challenge = state.challenge
        if (challenge == null) {
            if (state.gone) Text(stringResource(R.string.chess_problem_expired), style = PqType.Body, modifier = Modifier.padding(24.dp).testTag(IncomingTags.GONE))
            return@Column
        }
        val now = rememberTicking(running = true, phoneClock = { clock().toEpochMilli() })
        val left = (challenge.expiresAt.toEpochMilli() - now).coerceAtLeast(0)
        val open = challenge.isAnswerable(Instant.ofEpochMilli(now))
        val name = state.fromName ?: "?"
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .padding(PqDimens.ScreenPad)
                .fillMaxWidth()
                .comic(bg = PqColors.Paper, radius = PqDimens.RadiusLg, shadow = PqDimens.Shadow)
                .padding(18.dp)
        ) {
            InitialAvatar(name, 72.dp, ChessLook.avatar(0))
            Text(text = stringResource(R.string.chess_incoming_from, name).uppercase(), style = PqType.CardTitle, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            val colour = when (challenge.colour) {
                ColourChoice.WHITE -> stringResource(R.string.chess_you_play_black)
                ColourChoice.BLACK -> stringResource(R.string.chess_you_play_white)
                ColourChoice.RANDOM -> stringResource(R.string.chess_you_play_random)
            }
            Text(
                text = listOf(
                    stringResource(R.string.chess_minutes, challenge.timeControl.seconds / 60),
                    stringResource(if (challenge.rated) R.string.chess_rated else R.string.chess_friendly),
                    colour
                ).joinToString(" · "),
                style = PqType.MetaStrong
            )
            state.record?.let {
                Text(text = stringResource(R.string.chess_challenge_record, it.myWins, it.draws, it.theirWins), style = PqType.Body)
            }
            Text(
                text = if (open) stringResource(R.string.chess_incoming_left, com.pixelquest.app.chess.domain.ChessClock.format(left)) else stringResource(R.string.chess_problem_expired),
                style = PqType.BodyStrong,
                modifier = Modifier.testTag(IncomingTags.COUNTDOWN)
            )
            state.problem?.let { Text(text = problemText(it), style = PqType.Meta.copy(color = PqColors.Error)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PqButton(stringResource(R.string.chess_decline), onClick = { onAnswer(false) }, style = PqButtonStyle.Secondary, enabled = open && !state.answering, modifier = Modifier.weight(1f).testTag(IncomingTags.DECLINE))
                PqButton(stringResource(R.string.chess_accept), onClick = { onAnswer(true) }, enabled = open && !state.answering, modifier = Modifier.weight(1f).testTag(IncomingTags.ACCEPT))
            }
        }
    }
}
