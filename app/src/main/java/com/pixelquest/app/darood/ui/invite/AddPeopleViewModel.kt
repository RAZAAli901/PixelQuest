package com.pixelquest.app.darood.ui.invite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.MyUsername
import com.pixelquest.app.darood.data.UsernameRepository
import com.pixelquest.app.darood.data.remote.InviteOutcome
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyRole
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.darood.domain.UsernameProblem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** What a username search found, once the typing paused. */
sealed interface Lookup {
    /** Nothing typed, or still typing. */
    data object Idle : Lookup

    data object Checking : Lookup

    /** Someone has this username: they can be invited. */
    data class Found(val username: Username) : Lookup

    /** Nobody has this username. */
    data class NotFound(val username: Username) : Lookup

    /** It's the account's own. */
    data object Yours : Lookup

    /** It can't be a username at all. */
    data class Invalid(val problem: UsernameProblem) : Lookup

    /** The server couldn't be asked. */
    data object Unknown : Lookup
}

/** What happened to the last invitation sent. */
sealed interface InviteResult {
    data class Sent(val username: String) : InviteResult
    data class AlreadyIn(val username: String) : InviteResult
    data object Offline : InviteResult
    data class Refused(val refusal: DaroodRefusal) : InviteResult
}

/**
 * @param people everyone the party has asked or had: members, invitations and answers, for badges.
 * @param sending an invitation is on its way.
 */
data class AddPeopleUiState(
    val query: String = "",
    val lookup: Lookup = Lookup.Idle,
    val people: List<PartyMember> = emptyList(),
    val sending: Boolean = false,
    val result: InviteResult? = null,
    /** How many the party holds (its max_members), once known. */
    val limit: Int? = null
) {
    /** Places taken: members, and invitations until they're answered, as the server counts them. */
    val placesTaken: Int get() = people.count { it.status == MemberStatus.ACTIVE || it.status == MemberStatus.INVITED }

    /** No room for another invitation. */
    val isFull: Boolean get() = limit != null && placesTaken >= limit
}

/**
 * Adding people to a party by their username (docs/darood-design.md, Parties). Usernames can't be
 * listed, so a search is a lookup of one exact name: the server says only whether it's taken, and a
 * taken name is someone to invite.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class AddPeopleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usernames: UsernameRepository,
    private val parties: DaroodPartyRepository,
    private val myUsername: MyUsername? = null
) : ViewModel() {

    val partyId: String = savedStateHandle["partyId"] ?: ""

    private val _uiState = MutableStateFlow(AddPeopleUiState())
    val uiState: StateFlow<AddPeopleUiState> = _uiState.asStateFlow()

    private val typed = MutableStateFlow("")

    init {
        viewModelScope.launch { typed.debounce(LOOKUP_DELAY_MS).collectLatest(::lookUp) }
        // The party's people, the leader first, then by answer: who's in, who's asked, who said no.
        viewModelScope.launch {
            parties.observeMembers(partyId).collect { members ->
                _uiState.update { it.copy(people = members.sortedWith(compareBy({ it.role != PartyRole.LEADER }, { STATUS_ORDER.indexOf(it.status) }))) }
            }
        }
        viewModelScope.launch {
            parties.observeParty(partyId).collect { party -> _uiState.update { it.copy(limit = party?.maxMembers) } }
        }
        viewModelScope.launch { parties.refreshParty(partyId) }
    }

    /** Invites [username] to the party; the people list follows from the server. */
    fun invite(username: String) {
        // Full: the server would refuse it, so it isn't sent (the screen says why).
        if (_uiState.value.sending || _uiState.value.isFull) return
        _uiState.update { it.copy(sending = true, result = null) }
        viewModelScope.launch {
            val result = parties.invite(partyId, username)
            _uiState.update {
                it.copy(
                    sending = false,
                    result = when {
                        result is SupabaseResult.Success && result.data == InviteOutcome.ALREADY_MEMBER -> InviteResult.AlreadyIn(username)
                        result is SupabaseResult.Success -> InviteResult.Sent(username)
                        result is SupabaseResult.NetworkError -> InviteResult.Offline
                        else -> InviteResult.Refused(DaroodRefusal.of(result) ?: DaroodRefusal.INVALID)
                    }
                )
            }
        }
    }

    fun onQueryChange(raw: String) {
        val query = raw.trim().lowercase(Locale.ROOT)
        _uiState.update { it.copy(query = query, lookup = Lookup.Idle) }
        typed.value = query
    }

    private suspend fun lookUp(query: String) {
        if (query.isEmpty()) return
        val name = Username.parse(query)
        val outcome = when {
            name == null -> Username.problemWith(query)?.let { Lookup.Invalid(it) } ?: Lookup.Idle
            myUsername?.username?.first() == name -> Lookup.Yours
            else -> {
                setLookup(query, Lookup.Checking)
                when (val result = usernames.isUsernameAvailable(name)) {
                    is SupabaseResult.Success -> if (result.data) Lookup.NotFound(name) else Lookup.Found(name)
                    else -> Lookup.Unknown
                }
            }
        }
        setLookup(query, outcome)
    }

    /** An answer for a name since changed is dropped. */
    private fun setLookup(query: String, lookup: Lookup) =
        _uiState.update { if (it.query == query) it.copy(lookup = lookup) else it }

    companion object {
        /** How long typing must pause before the name is looked up. */
        const val LOOKUP_DELAY_MS = 400L

        private val STATUS_ORDER = listOf(MemberStatus.ACTIVE, MemberStatus.INVITED, MemberStatus.DECLINED, MemberStatus.LEFT, MemberStatus.REMOVED)
    }
}
