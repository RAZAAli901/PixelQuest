package com.pixelquest.app.darood.ui.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.darood.data.OwnUsername
import com.pixelquest.app.darood.data.SetUsernameResult
import com.pixelquest.app.darood.data.UsernameRepository
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.data.remote.SupabaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

/** What the server said about the name typed, once the typing paused. */
enum class UsernameAvailability { UNCHECKED, CHECKING, AVAILABLE, TAKEN, UNKNOWN }

/** Why a save didn't go through, for the screen to explain. */
sealed interface UsernameSaveError {
    data object Taken : UsernameSaveError
    data object Invalid : UsernameSaveError
    data class TooSoon(val changeableAt: Instant) : UsernameSaveError
    data object Offline : UsernameSaveError
}

/**
 * The username screen's state.
 *
 * @param input what's typed, in lowercase (how it will be stored).
 * @param problem why [input] can't be a username; null while empty or fine.
 * @param current the account's username so far, null before it has one.
 * @param saved the name just saved: the screen is done.
 */
data class UsernameSetupUiState(
    val input: String = "",
    val problem: UsernameProblem? = null,
    val availability: UsernameAvailability = UsernameAvailability.UNCHECKED,
    val current: OwnUsername? = null,
    val loadingCurrent: Boolean = true,
    val saving: Boolean = false,
    val saveError: UsernameSaveError? = null,
    val saved: Username? = null
) {
    /** The account's own name, typed again: nothing to save. */
    val isCurrent: Boolean get() = current?.username?.value == input

    val canSave: Boolean
        get() = !saving && !isCurrent && problem == null && input.isNotEmpty() && availability == UsernameAvailability.AVAILABLE
}

/**
 * Claiming or changing the Darood Party username (docs/darood-design.md, Usernames). The name is
 * checked as it's typed: its form at once, its availability with the server once the typing has
 * paused for [CHECK_DELAY_MS]. The server has the last word when it's saved.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class UsernameSetupViewModel @Inject constructor(
    private val repository: UsernameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsernameSetupUiState())
    val uiState: StateFlow<UsernameSetupUiState> = _uiState.asStateFlow()

    private val typed = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val current = (repository.fetchMyUsername() as? SupabaseResult.Success)?.data
            _uiState.update { state ->
                // Changing a name starts from the current one, unless typing has begun.
                val input = if (state.input.isEmpty() && current != null) current.username.value else state.input
                state.copy(current = current, loadingCurrent = false, input = input)
            }
        }
        viewModelScope.launch {
            typed.debounce(CHECK_DELAY_MS).collectLatest { input -> check(input) }
        }
    }

    fun onInputChange(raw: String) {
        val input = com.pixelquest.app.domain.validation.InputRules.typed(raw, com.pixelquest.app.domain.validation.InputRules.Field.USERNAME).trim().lowercase(Locale.ROOT)
        _uiState.update {
            it.copy(
                input = input,
                problem = if (input.isEmpty()) null else Username.problemWith(input),
                availability = UsernameAvailability.UNCHECKED,
                saveError = null
            )
        }
        typed.value = input
    }

    private suspend fun check(input: String) {
        val name = Username.parse(input) ?: return
        if (_uiState.value.current?.username == name) return
        _uiState.update { if (it.input == input) it.copy(availability = UsernameAvailability.CHECKING) else it }
        val availability = when (val result = repository.isUsernameAvailable(name)) {
            is SupabaseResult.Success -> if (result.data) UsernameAvailability.AVAILABLE else UsernameAvailability.TAKEN
            else -> UsernameAvailability.UNKNOWN
        }
        // An answer for a name since changed is dropped.
        _uiState.update { if (it.input == input) it.copy(availability = availability) else it }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val name = Username.parse(state.input) ?: return
        _uiState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val result = repository.setUsername(name)
            _uiState.update {
                when (result) {
                    is SupabaseResult.Success -> when (val outcome = result.data) {
                        is SetUsernameResult.Saved -> it.copy(
                            saving = false,
                            saved = outcome.username,
                            current = OwnUsername(outcome.username, outcome.changeableAt)
                        )
                        SetUsernameResult.Taken -> it.copy(saving = false, availability = UsernameAvailability.TAKEN, saveError = UsernameSaveError.Taken)
                        SetUsernameResult.Invalid -> it.copy(saving = false, saveError = UsernameSaveError.Invalid)
                        is SetUsernameResult.TooSoon -> it.copy(saving = false, saveError = UsernameSaveError.TooSoon(outcome.changeableAt))
                    }
                    else -> it.copy(saving = false, saveError = UsernameSaveError.Offline)
                }
            }
        }
    }

    companion object {
        /** How long typing must pause before the name is checked with the server. */
        const val CHECK_DELAY_MS = 400L
    }
}
