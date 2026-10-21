package com.pixelquest.app.darood.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.party.PartyKinds
import com.pixelquest.app.party.domain.PartyKind
import com.pixelquest.app.party.domain.PartyType

/** Why the list couldn't be brought up to date, for the screen to say. */
enum class PartiesProblem {
    /** The server couldn't be reached: the list is what the phone has. */
    OFFLINE,

    /** The server said no to an answer (the invitation was withdrawn, or the party is full or finished). */
    INVITE_GONE,

    /** Something else went wrong. */
    FAILED
}

/**
 * The Parties hub (UI v2): the parties of every kind the account is in, or of one kind.
 *
 * @param signedIn null while the saved session loads.
 * @param parties the parties the account is in, by name.
 * @param invites parties it's invited to, waiting for an answer.
 * @param loading a refresh the player pulled for is under way (the circle); the catch-up on opening
 *   is silent, as the list is already on screen from the phone.
 * @param answering the invitation being answered, while it is.
 */
data class DaroodPartiesUiState(
    val signedIn: Boolean? = null,
    val parties: List<Party> = emptyList(),
    val invites: List<Party> = emptyList(),
    val loading: Boolean = false,
    val problem: PartiesProblem? = null,
    val answering: String? = null
) {
    val isEmpty: Boolean get() = parties.isEmpty() && invites.isEmpty()
}

@HiltViewModel
class DaroodPartiesViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val repository: DaroodPartyRepository,
    private val reminder: com.pixelquest.app.darood.data.DaroodReminderSettings? = null,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    myUsername: com.pixelquest.app.darood.data.MyUsername? = null,
    settings: com.pixelquest.app.domain.repository.SettingsRepository? = null
) : ViewModel() {

    /** The kind of party this list shows, if its route names one; every kind without ([ARG_TYPE]). */
    val type: PartyType? = savedStateHandle.get<String>(ARG_TYPE)?.let { PartyType.fromWire(it) }

    /** Darood Parties show while Darood is available ([SettingsRepository.daroodAvailable]). */
    val daroodAvailable: StateFlow<Boolean> = (settings?.daroodAvailable ?: kotlinx.coroutines.flow.flowOf(true))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** The account's username, for the hub's footer and its Share button; null without one. */
    val username: StateFlow<String?> = (myUsername?.username?.map { it?.value } ?: kotlinx.coroutines.flow.flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The weekly note, off unless the player turns it on. */
    val weeklyReminder: StateFlow<Boolean> = reminder?.weeklyReminder ?: kotlinx.coroutines.flow.MutableStateFlow(false)

    fun setWeeklyReminder(on: Boolean) {
        reminder?.setWeeklyReminder(on)
    }


    private val status = MutableStateFlow(DaroodPartiesUiState())

    private val signedIn = authRepository.currentUser.map { it != null }.distinctUntilChanged()

    /**
     * Invitations to a party of any kind, waiting for an answer: the Parties screen's inbox, where
     * every invitation arrives, whatever its kind. Empty signed out.
     */
    val allInvites: StateFlow<List<Party>> = combine(signedIn, repository.observeMyParties()) { signedIn, all ->
        if (signedIn) all.filter { it.myStatus == MemberStatus.INVITED } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<DaroodPartiesUiState> = combine(
        signedIn,
        if (type != null) repository.observeMyParties(type) else repository.observeMyParties(),
        daroodAvailable,
        status
    ) { signedIn, all, darood, status ->
        val shown = all.filter { darood || it.type != PartyType.DAROOD }
        status.copy(
            signedIn = signedIn,
            parties = if (signedIn) shown.filter { it.myStatus == MemberStatus.ACTIVE } else emptyList(),
            invites = if (signedIn) shown.filter { it.myStatus == MemberStatus.INVITED } else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DaroodPartiesUiState())

    init {
        // Each sign-in (and the first launch signed in) brings the list from the server.
        viewModelScope.launch { signedIn.collect { if (it) sync(shown = false) } }
    }

    private var sync: Job? = null

    /** Pull-to-refresh: the list from the server, with the circle. */
    fun refresh() = sync(shown = true)

    private fun sync(shown: Boolean) {
        if (shown) status.update { it.copy(loading = true) }
        val running = sync
        if (running != null && running.isActive) {
            // One is under way: the pull waits for it rather than asking the server twice.
            if (shown) running.invokeOnCompletion { status.update { it.copy(loading = false) } }
            return
        }
        sync = viewModelScope.launch {
            val result = repository.refreshParties()
            status.update { it.copy(loading = false, problem = problemOf(result)) }
        }
    }

    fun accept(partyId: String) = answer(partyId, accept = true)

    fun decline(partyId: String) = answer(partyId, accept = false)

    private fun answer(partyId: String, accept: Boolean) {
        if (status.value.answering != null) return
        viewModelScope.launch {
            status.update { it.copy(answering = partyId, problem = null) }
            val result = repository.respondToInvite(partyId, accept)
            // An invitation that's gone (withdrawn, the party deleted): the list drops it.
            if (result !is SupabaseResult.Success) repository.refreshParties()
            status.update { it.copy(answering = null, problem = problemOf(result)) }
        }
    }

    /** Follows the account's invitations live until cancelled: collect it while a screen shows them. */
    suspend fun followInvitations() {
        repository.followInvitations().collect { }
    }

    fun dismissProblem() = status.update { it.copy(problem = null) }

    private fun problemOf(result: SupabaseResult<*>): PartiesProblem? = when {
        result is SupabaseResult.Success -> null
        result is SupabaseResult.NetworkError -> PartiesProblem.OFFLINE
        DaroodRefusal.of(result) != null -> PartiesProblem.INVITE_GONE
        else -> PartiesProblem.FAILED
    }

    companion object {
        /** The route's argument: the kind of party listed (PartyType's wire name); every kind without it. */
        const val ARG_TYPE = "type"
    }
}
