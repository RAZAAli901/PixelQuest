package com.pixelquest.app.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Immutable state rendered by a screen composable.
 */
interface UiState

/**
 * User action or intent sent from the UI composable to the ViewModel.
 */
interface UiEvent

/**
 * One-off side effect (navigation, snackbar, toast) emitted by the ViewModel.
 */
interface UiEffect

/**
 * Unified contract for Unidirectional Data Flow (UDF) across PixelQuest ViewModels.
 */
interface MviContract<State : UiState, in Event : UiEvent, Effect : UiEffect> {
    val state: StateFlow<State>
    val effects: Flow<Effect>
    fun onEvent(event: Event)
}

/**
 * Base ViewModel providing structured state reduction and channel-backed one-off effects.
 */
abstract class BaseViewModel<State : UiState, Event : UiEvent, Effect : UiEffect>(
    initialState: State,
) : ViewModel(),
    MviContract<State, Event, Effect> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State> = _state.asStateFlow()
    protected val currentState: State get() = _state.value

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    override fun onEvent(event: Event) {
        handleEvent(event)
    }

    protected abstract fun handleEvent(event: Event)

    protected fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    protected fun updateState(reducer: State.() -> State) {
        _state.value = _state.value.reducer()
    }
}
