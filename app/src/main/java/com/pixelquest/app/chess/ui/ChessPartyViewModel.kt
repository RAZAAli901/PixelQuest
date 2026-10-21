package com.pixelquest.app.chess.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.chess.data.ChessChallengeRepository
import com.pixelquest.app.chess.data.ChessRatingRepository
import com.pixelquest.app.chess.data.SpectateRepository
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.domain.MemberStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Someone in the party the player can play. */
data class ChessMember(val userId: String, val name: String)

data class ChessPartyState(
    val me: String? = null,
    /** The party's other active members. */
    val members: List<ChessMember> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val liveGames: List<ChessGame> = emptyList(),
    val leaderboard: List<Rating> = emptyList(),
    /** The board's time control (null: each member's best). */
    val boardTimeControl: TimeControl? = null,
    /** The record against whoever's picked in the challenge sheet. */
    val preview: List<HeadToHead> = emptyList(),
    val sending: Boolean = false,
    val problem: String? = null,
    val names: Map<String, String?> = emptyMap(),
    /** Games the chat's cards have asked about, by id. */
    val games: Map<String, ChessGame> = emptyMap(),
    /** Each member's best Int points, for their badge (J158). */
    val best: Map<String, Int> = emptyMap()
)

/** What a challenge became, for the screen to open. */
sealed interface ChessPartyEvent {
    data class Sent(val challenge: Challenge) : ChessPartyEvent
    data class GameStarted(val gameId: String) : ChessPartyEvent
}

/**
 * Chess in one study party (J150 on): its members to challenge, the open challenges, the games
 * being played, and the party's leaderboard. Everything goes to the server, which checks it.
 */
@HiltViewModel
class ChessPartyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val challengesRepo: ChessChallengeRepository,
    private val ratings: ChessRatingRepository,
    private val spectate: SpectateRepository,
    private val party: PartyRepository,
    account: DaroodAccount,
    private val chess: com.pixelquest.app.chess.data.ChessRepository? = null
) : ViewModel() {
    val partyId: String = savedStateHandle[ARG_PARTY] ?: ""
    private val me = account.id()
    private val _state = MutableStateFlow(ChessPartyState(me = me))
    val state: StateFlow<ChessPartyState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<ChessPartyEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ChessPartyEvent> = _events

    init {
        viewModelScope.launch {
            party.observeMembers(partyId).collect { members ->
                val active = members.filter { it.status == MemberStatus.ACTIVE }
                _state.update { s ->
                    s.copy(
                        members = active.filter { it.userId != me }.map { ChessMember(it.userId, it.username ?: it.userId.take(6)) },
                        names = members.associate { it.userId to it.username }
                    )
                }
            }
        }
        viewModelScope.launch { challengesRepo.observeChallenges(partyId).collect { list -> _state.update { it.copy(challenges = list) } } }
        refresh()
    }

    /** The challenges, live games and board from the server. */
    fun refresh() {
        viewModelScope.launch { challengesRepo.refreshChallenges(partyId) }
        viewModelScope.launch { (spectate.liveGames(partyId) as? SupabaseResult.Success)?.data?.let { games -> _state.update { it.copy(liveGames = games) } } }
        loadBoard(_state.value.boardTimeControl)
        viewModelScope.launch {
            (ratings.leaderboard(partyId, null) as? SupabaseResult.Success)?.data?.let { rows -> _state.update { it.copy(best = rows.associate { r -> r.userId to r.points }) } }
        }
    }

    fun loadBoard(timeControl: TimeControl?) {
        _state.update { it.copy(boardTimeControl = timeControl) }
        viewModelScope.launch {
            (ratings.leaderboard(partyId, timeControl) as? SupabaseResult.Success)?.data?.let { rows -> _state.update { it.copy(leaderboard = rows) } }
        }
    }

    private val asked = mutableSetOf<String>()

    /** A game a chat card is about: read once, then kept. */
    fun needGame(gameId: String) {
        val chess = chess ?: return
        if (!asked.add(gameId)) return
        viewModelScope.launch {
            (chess.fetchGame(gameId) as? SupabaseResult.Success)?.data?.first?.let { game -> _state.update { it.copy(games = it.games + (game.id to game)) } }
        }
    }

    /** The record against [other], shown as soon as they're picked. */
    fun preview(other: String?) {
        if (other == null) {
            _state.update { it.copy(preview = emptyList()) }
            return
        }
        viewModelScope.launch {
            val record = (ratings.headToHead(other) as? SupabaseResult.Success)?.data.orEmpty()
            _state.update { it.copy(preview = record) }
        }
    }

    /** Sends a challenge to [to], or to the whole room when null. */
    fun challenge(to: String?, timeControl: TimeControl, colour: ColourChoice, rated: Boolean) {
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, problem = null) }
        viewModelScope.launch {
            when (val sent = challengesRepo.challenge(partyId, to, timeControl, colour, rated)) {
                is SupabaseResult.Success -> {
                    _state.update { it.copy(sending = false) }
                    _events.tryEmit(ChessPartyEvent.Sent(sent.data))
                }
                is SupabaseResult.ServerError -> _state.update { it.copy(sending = false, problem = sent.message) }
                else -> _state.update { it.copy(sending = false, problem = ChessSessionViewModel.OFFLINE) }
            }
        }
    }

    /** Accepts (a game starts) or declines a challenge; the challenger's own "no" cancels it. */
    fun answer(challengeId: String, accept: Boolean) {
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, problem = null) }
        viewModelScope.launch {
            when (val answer = challengesRepo.respond(challengeId, accept)) {
                is SupabaseResult.Success -> {
                    _state.update { it.copy(sending = false) }
                    answer.data?.let { _events.tryEmit(ChessPartyEvent.GameStarted(it.id)) }
                }
                is SupabaseResult.ServerError -> _state.update { it.copy(sending = false, problem = answer.message) }
                else -> _state.update { it.copy(sending = false, problem = ChessSessionViewModel.OFFLINE) }
            }
            challengesRepo.refreshChallenges(partyId)
        }
    }

    companion object {
        const val ARG_PARTY = "partyId"
    }
}
