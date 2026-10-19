package com.pixelquest.app.darood.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
 * Darood Party's home.
 *
 * @param signedIn null while the saved session loads.
 * @param parties the parties the account is in, by name.
 * @param invites parties it's invited to, waiting for an answer.
 * @param loading a refresh is under way.
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
    private val reminder: com.pixelquest.app.darood.data.DaroodReminderSettings? = null
) : ViewModel() {

    /** The weekly note, off unless the player turns it on. */
    val weeklyReminder: StateFlow<Boolean> = reminder?.weeklyReminder ?: kotlinx.coroutines.flow.MutableStateFlow(false)

    fun setWeeklyReminder(on: Boolean) {
        reminder?.setWeeklyReminder(on)
    }


    private val status = MutableStateFlow(DaroodPartiesUiState())

    private val signedIn = authRepository.currentUser.map { it != null }.distinctUntilChanged()

    val uiState: StateFlow<DaroodPartiesUiState> = combine(signedIn, repository.observeMyParties(), status) { signedIn, all, status ->
        status.copy(
            signedIn = signedIn,
            parties = if (signedIn) all.filter { it.myStatus == MemberStatus.ACTIVE } else emptyList(),
            invites = if (signedIn) all.filter { it.myStatus == MemberStatus.INVITED } else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DaroodPartiesUiState())

    init {
        // Each sign-in (and the first launch signed in) brings the list from the server.
        viewModelScope.launch { signedIn.collect { if (it) refresh() } }
    }

    fun refresh() {
        viewModelScope.launch {
            status.update { it.copy(loading = true) }
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

    fun dismissProblem() = status.update { it.copy(problem = null) }

    private fun problemOf(result: SupabaseResult<*>): PartiesProblem? = when {
        result is SupabaseResult.Success -> null
        result is SupabaseResult.NetworkError -> PartiesProblem.OFFLINE
        DaroodRefusal.of(result) != null -> PartiesProblem.INVITE_GONE
        else -> PartiesProblem.FAILED
    }
}
