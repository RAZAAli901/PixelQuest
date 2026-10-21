package com.pixelquest.app.darood.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.DaroodRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.party.PartyKinds
import com.pixelquest.app.party.domain.PartyType

/** What's wrong with a party's name, as typed. */
enum class PartyNameProblem { TOO_SHORT, TOO_LONG }

/** Why creating didn't work. */
enum class CreatePartyError {
    /** The account has no username yet: members are shown, and invited, by username. */
    USERNAME_REQUIRED,

    /** The server couldn't be reached: nothing was created. */
    OFFLINE,

    /** The server refused it or something else went wrong. */
    FAILED
}

/**
 * Starting a party. [name] is kept as typed; the server trims it, and so does the length rule here.
 */
data class CreatePartyUiState(
    val name: String = "",
    /** How many the party holds, the leader included: 2 to 30. */
    val maxMembers: Int = DaroodRules.MEMBERS_DEFAULT,
    /** The milestone as typed: digits only, empty for none yet. */
    val milestoneText: String = "",
    val creating: Boolean = false,
    val error: CreatePartyError? = null,
    /** The new party's id, once it's made: the screen moves on to it. */
    val created: String? = null,
    /** The kind of party it starts: chosen on the form; a Darood Party unless the route says otherwise. */
    val type: PartyType = PartyType.DAROOD,
    /** The kinds that can be started: Darood only while it's available. */
    val types: List<PartyType> = PartyType.entries
) {
    /** Its kind has a milestone, asked for here (optional). */
    val asksMilestone: Boolean get() = PartyKinds.of(type).rules.milestone

    /** Everything is as the server will accept it, and nothing is on its way. */
    val canCreate: Boolean get() = nameIsValid && !(asksMilestone && milestoneIsZero) && !creating && created == null

    /** The milestone to start with, or null for none yet (it can be set later). */
    val milestone: Long? get() = milestoneText.toLongOrNull()

    /** A milestone of 0 isn't one: it must be above 0 (or left empty). */
    val milestoneIsZero: Boolean get() = milestone == 0L

    val canHaveFewer: Boolean get() = maxMembers > DaroodRules.MEMBERS_MIN
    val canHaveMore: Boolean get() = maxMembers < DaroodRules.MEMBERS_MAX

    /** The name's length as it will be stored (without spaces at either end). */
    val nameLength: Int get() = name.trim().length

    /** Why the name can't be a party's, once something's typed; null while empty or fine. */
    val nameProblem: PartyNameProblem?
        get() = when {
            name.isEmpty() -> null
            nameLength < DaroodRules.NAME_MIN -> PartyNameProblem.TOO_SHORT
            nameLength > DaroodRules.NAME_MAX -> PartyNameProblem.TOO_LONG
            else -> null
        }

    val nameIsValid: Boolean get() = nameLength in DaroodRules.NAME_MIN..DaroodRules.NAME_MAX
}

@HiltViewModel
class CreatePartyViewModel @Inject constructor(
    private val repository: DaroodPartyRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    settings: com.pixelquest.app.domain.repository.SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePartyUiState(type = PartyType.fromWire(savedStateHandle[ARG_TYPE])))
    val uiState: StateFlow<CreatePartyUiState> = _uiState.asStateFlow()

    init {
        // Without Darood, a study party is all there is to start.
        settings?.let { repo ->
            viewModelScope.launch {
                repo.daroodAvailable.collect { darood ->
                    _uiState.update { state ->
                        val types = if (darood) PartyType.entries else PartyType.entries - PartyType.DAROOD
                        state.copy(types = types, type = state.type.takeIf { it in types } ?: types.first())
                    }
                }
            }
        }
    }

    /** Picks the kind of party: Darood with its milestone, or study. */
    fun setType(type: PartyType) = _uiState.update { if (type in it.types) it.copy(type = type) else it }

    /** A little past the limit can still be typed, so the counter can say it's too long. */
    fun onNameChange(name: String) = _uiState.update { it.copy(name = name.take(DaroodRules.NAME_MAX + 10)) }

    fun setMaxMembers(count: Int) = _uiState.update { it.copy(maxMembers = count.coerceIn(DaroodRules.MEMBERS_MIN, DaroodRules.MEMBERS_MAX)) }

    fun fewerMembers() = setMaxMembers(_uiState.value.maxMembers - 1)

    fun moreMembers() = setMaxMembers(_uiState.value.maxMembers + 1)

    /** Creates the party with the account as its leader; on success [CreatePartyUiState.created] is its id. */
    fun create() {
        val state = _uiState.value
        if (!state.canCreate) return
        _uiState.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            // A Darood Party starts as it always has, with its milestone; another kind with its name and size.
            val result = if (state.asksMilestone) {
                repository.createParty(state.name.trim(), state.maxMembers, state.milestone)
            } else {
                repository.createParty(state.name.trim(), state.maxMembers, state.type)
            }
            _uiState.update {
                when {
                    result is SupabaseResult.Success -> it.copy(creating = false, created = result.data)
                    result is SupabaseResult.NetworkError -> it.copy(creating = false, error = CreatePartyError.OFFLINE)
                    DaroodRefusal.of(result) == DaroodRefusal.USERNAME_REQUIRED -> it.copy(creating = false, error = CreatePartyError.USERNAME_REQUIRED)
                    else -> it.copy(creating = false, error = CreatePartyError.FAILED)
                }
            }
        }
    }

    /** Only digits are kept (a paste of "10,000 Darood" keeps 10000), at most [MILESTONE_DIGITS]. */
    fun onMilestoneChange(text: String) =
        _uiState.update { it.copy(milestoneText = text.filter(Char::isDigit).trimStart('0').ifEmpty { if (text.any(Char::isDigit)) "0" else "" }.take(MILESTONE_DIGITS)) }

    companion object {
        /** The route's argument: the kind of party to start (PartyType's wire name); a Darood Party without it. */
        const val ARG_TYPE = "type"

        /** Up to 9,999,999,999: far beyond any party's reach, and well inside the server's limit. */
        const val MILESTONE_DIGITS = 10
    }
}
