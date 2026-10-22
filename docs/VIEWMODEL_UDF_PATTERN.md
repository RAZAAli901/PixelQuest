# ViewModel State, Event, and Effect Architecture

To ensure consistency, readability, and testability across all feature modules, PixelQuest enforces the **UiState, Event, and Effect** pattern (Unidirectional Data Flow / MVI) defined in `com.pixelquest.app.ui.base.UiContract.kt`.

## Core Concepts

### 1. `UiState`
- Represents everything required to render the screen at a single point in time.
- Must be an immutable data class or sealed hierarchy.
- Exposed exclusively as a read-only `StateFlow<UiState>`.
- Composables observe state via `collectAsStateWithLifecycle()` or `collectAsState()`.

### 2. `UiEvent`
- Represents intentional user interactions (e.g. `ClickTab(tab)`, `SubmitPrayer(namaz)`, `ToggleTask(id)`).
- Sent from composables to the ViewModel via `viewModel.onEvent(event)`.
- Eliminates ad-hoc public mutation methods on ViewModels.

### 3. `UiEffect`
- Represents single-time actions that must not survive configuration changes or recompositions:
  - Navigation actions (`NavigateTo(route)`)
  - Ephemeral user feedback (`ShowSnackbar(message)`, `TriggerHaptic`)
  - Intent launches (`OpenExternalBrowser(url)`)
- Backed by an unbuffered or buffered coroutine `Channel` and exposed as `Flow<UiEffect>`.

## Contract Definition

```kotlin
interface UiState
interface UiEvent
interface UiEffect

interface MviContract<State : UiState, in Event : UiEvent, Effect : UiEffect> {
    val state: StateFlow<State>
    val effects: Flow<Effect>
    fun onEvent(event: Event)
}
```

## Implementation Guide

ViewModels can inherit from `BaseViewModel`:

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val useCase: ExampleUseCase
) : BaseViewModel<ExampleUiState, ExampleUiEvent, ExampleUiEffect>(ExampleUiState()) {

    override fun handleEvent(event: ExampleUiEvent) {
        when (event) {
            is ExampleUiEvent.Refresh -> reload()
            is ExampleUiEvent.SelectItem -> emitEffect(ExampleUiEffect.NavigateToDetail(event.id))
        }
    }
}
```

Or implement `MviContract` directly for customized flow combinations (such as `combine(...).stateIn(...)`).
