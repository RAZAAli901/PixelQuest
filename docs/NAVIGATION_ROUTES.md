# Type-Safe Navigation Architecture

## Overview
PixelQuest uses Jetpack Compose Navigation with centralized type-safe route definitions and typed argument extractors in `Screen` objects (`Routes.kt`).

## Design Pattern

### 1. Centralized Route Definitions (`Screen`)
Every screen destination inherits from `Screen(val route: String)` and specifies:
- `arguments`: The list of `NamedNavArgument` needed by the route (defaults to empty).
- Argument key constants (e.g., `ARG_TASK_ID`, `ARG_PARTY_ID`).
- `createRoute(...)`: Type-safe route builder requiring mandatory arguments.
- Strongly-typed argument extractors for both `NavBackStackEntry` and `SavedStateHandle`:
  - `getTaskId(entry: NavBackStackEntry): Long`
  - `getTaskId(handle: SavedStateHandle): Long`
  - `parseArgs(entry: NavBackStackEntry): Args` (for multi-parameter routes)

### 2. Destination Graph Registration
In `NavGraphBuilder` composables, destinations declare their arguments by referencing the Screen's declared arguments directly rather than duplicating parameter lists:

```kotlin
composable(
    route = Screen.EditTask.route,
    arguments = Screen.EditTask.arguments
) { backStackEntry ->
    val taskId = Screen.EditTask.getTaskId(backStackEntry)
    ...
}
```

### 3. ViewModel SavedStateHandle Resolution
ViewModels extracting arguments directly from `SavedStateHandle` use the helper parser:

```kotlin
@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    ...
) : ViewModel() {
    val taskId: Long = Screen.EditTask.getTaskId(savedStateHandle)
}
```

## Benefits
- **Zero hard-coded argument keys in composable registrations**: Argument definitions live with the route.
- **Fail-fast compile-time safety**: Route generation prevents malformed URL paths or missing required arguments.
- **Unified tests**: `RoutesTest` asserts route format parity and argument parsing accuracy.
