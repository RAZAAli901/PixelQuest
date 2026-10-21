package com.pixelquest.app.darood.ui.weekly

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.domain.WeekResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A Darood Party's weekly ranking (docs/darood-weekly.md): its closed weeks, newest first, as the
 * phone has them, with every ranked member's name. [refreshed] is false until the server's answered.
 */
data class WeeklyRankingState(
    val weeks: List<WeekResult> = emptyList(),
    val names: Map<String, String?> = emptyMap(),
    val me: String? = null,
    val refreshed: Boolean = false,
    val offline: Boolean = false
) {
    val latest: WeekResult? get() = weeks.firstOrNull()
}

@HiltViewModel
class WeeklyRankingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DaroodPartyRepository,
    account: DaroodAccount
) : ViewModel() {
    val partyId: String = savedStateHandle["partyId"] ?: ""
    private val me = account.id()
    private val answer = MutableStateFlow<Boolean?>(null)

    val state: StateFlow<WeeklyRankingState> = combine(
        repository.observeWeekResults(partyId), repository.observeMembers(partyId), answer
    ) { weeks, members, answered ->
        val names = members.associate { it.userId to it.username }
        WeeklyRankingState(weeks, names, me, refreshed = answered != null, offline = answered == false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyRankingState(me = me))

    init {
        refresh()
    }

    /** Closes last week if it's over (stored once on the server), then reads the recent weeks. */
    fun refresh() {
        viewModelScope.launch {
            repository.closeLastWeek(partyId)
            answer.value = repository.fetchWeekResults(partyId) is SupabaseResult.Success
        }
    }
}
