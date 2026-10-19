package com.pixelquest.app.darood.ui.create

import androidx.lifecycle.ViewModel
import com.pixelquest.app.darood.domain.DaroodRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** What's wrong with a party's name, as typed. */
enum class PartyNameProblem { TOO_SHORT, TOO_LONG }

/**
 * Starting a party. [name] is kept as typed; the server trims it, and so does the length rule here.
 */
data class CreatePartyUiState(
    val name: String = "",
    /** How many the party holds, the leader included: 2 to 30. */
    val maxMembers: Int = DaroodRules.MEMBERS_DEFAULT,
    /** The milestone as typed: digits only, empty for none yet. */
    val milestoneText: String = ""
) {
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
class CreatePartyViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePartyUiState())
    val uiState: StateFlow<CreatePartyUiState> = _uiState.asStateFlow()

    /** A little past the limit can still be typed, so the counter can say it's too long. */
    fun onNameChange(name: String) = _uiState.update { it.copy(name = name.take(DaroodRules.NAME_MAX + 10)) }

    fun setMaxMembers(count: Int) = _uiState.update { it.copy(maxMembers = count.coerceIn(DaroodRules.MEMBERS_MIN, DaroodRules.MEMBERS_MAX)) }

    fun fewerMembers() = setMaxMembers(_uiState.value.maxMembers - 1)

    fun moreMembers() = setMaxMembers(_uiState.value.maxMembers + 1)

    /** Only digits are kept (a paste of "10,000 Darood" keeps 10000), at most [MILESTONE_DIGITS]. */
    fun onMilestoneChange(text: String) =
        _uiState.update { it.copy(milestoneText = text.filter(Char::isDigit).trimStart('0').ifEmpty { if (text.any(Char::isDigit)) "0" else "" }.take(MILESTONE_DIGITS)) }

    companion object {
        /** Up to 9,999,999,999: far beyond any party's reach, and well inside the server's limit. */
        const val MILESTONE_DIGITS = 10
    }
}
