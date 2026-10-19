package com.pixelquest.app.darood.ui.invite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.MyUsername
import com.pixelquest.app.darood.data.UsernameRepository
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

data class AddPeopleUiState(
    val query: String = "",
    val lookup: Lookup = Lookup.Idle
)

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
    private val myUsername: MyUsername? = null
) : ViewModel() {

    val partyId: String = savedStateHandle["partyId"] ?: ""

    private val _uiState = MutableStateFlow(AddPeopleUiState())
    val uiState: StateFlow<AddPeopleUiState> = _uiState.asStateFlow()

    private val typed = MutableStateFlow("")

    init {
        viewModelScope.launch { typed.debounce(LOOKUP_DELAY_MS).collectLatest(::lookUp) }
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
    }
}
