# Coroutine Scope Lifecycle and Ownership Policy

PixelQuest strictly prohibits the use of unmanaged or global coroutine scopes (`GlobalScope`). Every coroutine scope must have a clearly defined lifecycle owner and proper cancellation semantics.

## Scope Ownership Rules

1. **ViewModels**:
   - Must strictly execute asynchronous operations within `viewModelScope`.
   - Never create independent `CoroutineScope` instances inside a ViewModel.

2. **Composables**:
   - Must use `rememberCoroutineScope()` bound to the composable lifecycle.
   - Long-lived background observation in UI must use `LaunchedEffect` or lifecycle-aware collection (`collectAsStateWithLifecycle()`).

3. **Android Services (e.g. `PushService`)**:
   - Service-level scopes must be constructed with a `SupervisorJob()`:
     ```kotlin
     private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
     ```
   - The scope must be explicitly cancelled when the service is destroyed:
     ```kotlin
     override fun onDestroy() {
         super.onDestroy()
         scope.cancel()
     }
     ```

4. **Broadcast Receivers**:
   - For asynchronous operations in receivers, always call `goAsync()` to retain the process until completion.
   - The `BroadcastReceiver.PendingResult.finish()` call must be placed inside a `finally` block to prevent ANR / execution leaks.

5. **Singletons and Observers (e.g. `ConnectivitySyncObserver`)**:
   - Background observers running for the duration of the component must use `SupervisorJob()`.
   - When the observer stops (`stopObserving()`), child coroutines must be cancelled via `scope.coroutineContext.cancelChildren()`.
