package com.pixelquest.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What the app's frame shows besides the screens: which tabs, their wording, and their dots. */
data class PqRootState(
    /** Simple Mode: plain wording, no game terms. */
    val isSimpleMode: Boolean = false,
    /** The Deen tab shows: on unless the player hid the Islamic section in Settings. */
    val islamicHubShown: Boolean = true,
    /** A Darood Party invitation is waiting, while Darood is available: the Deen tab's dot. */
    val daroodInviteWaiting: Boolean = false,
    /** An invitation to a party of any kind is waiting: the parties buttons' dot. */
    val partyInviteWaiting: Boolean = false,
)

@HiltViewModel
class PqRootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    daroodParties: DaroodPartyRepository,
) : ViewModel() {

    val state: StateFlow<PqRootState> = combine(
        settingsRepository.simpleModeEnabled,
        settingsRepository.islamicHubShown,
        settingsRepository.daroodAvailable,
        daroodParties.observeMyParties().map { parties -> parties.filter { it.myStatus == MemberStatus.INVITED } },
    ) { simple, islamic, darood, invited ->
        PqRootState(
            isSimpleMode = simple,
            islamicHubShown = islamic,
            daroodInviteWaiting = darood && invited.any { it.type == PartyType.DAROOD },
            partyInviteWaiting = invited.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PqRootState())
}
