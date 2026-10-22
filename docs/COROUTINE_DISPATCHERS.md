# Coroutine Dispatcher Injection

PixelQuest enforces injected coroutine dispatchers instead of hardcoding `Dispatchers.IO`, `Dispatchers.Default`, or `Dispatchers.Main`. Injecting dispatchers allows unit tests to supply `StandardTestDispatcher` or `UnconfinedTestDispatcher` without flakiness or uncontrolled background execution.

## Dispatcher Providers

Defined in `com.pixelquest.app.di.DispatchersModule.kt`:

1. **Qualifier Annotations**:
   - `@IoDispatcher`: For disk, Room, filesystem, and network I/O.
   - `@DefaultDispatcher`: For CPU-heavy transformations, sorting, and JSON parsing.
   - `@MainDispatcher`: For UI-thread interactions and Compose bindings.

2. **Container Object**:
   - `CoroutineDispatchers(val io: CoroutineDispatcher, val default: CoroutineDispatcher, val main: CoroutineDispatcher)`

## Usage Examples

### Injecting Specific Dispatcher

```kotlin
@Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun loadData() = withContext(ioDispatcher) { ... }
}
```

### Injecting Dispatchers Container

```kotlin
@Inject constructor(
    private val dispatchers: CoroutineDispatchers,
) {
    suspend fun process() = withContext(dispatchers.default) { ... }
}
```

### In Tests

Unit tests supply test dispatchers directly:

```kotlin
val testDispatcher = StandardTestDispatcher()
val dispatchers = CoroutineDispatchers(
    io = testDispatcher,
    default = testDispatcher,
    main = testDispatcher,
)
```
