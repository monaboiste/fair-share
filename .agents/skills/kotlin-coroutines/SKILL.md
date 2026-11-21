---
name: kotlin-coroutines
description: >
  Kotlin coroutines and Flow patterns. Use when writing or testing suspend functions, coroutine
  scopes, dispatchers, or cancellation, or Flow, StateFlow, or SharedFlow streams.
---

# Kotlin coroutines

Use these patterns when the feature needs coroutine APIs; follow the application's existing execution model.

## Ownership and concurrency

- Expose a suspend function for one asynchronous result and `Flow<T>` for a stream. A `suspend` modifier does not
  move work to another thread or make a blocking call non-blocking.
- Keep child work inside `coroutineScope`. Start independent tasks with `async` before awaiting them; keep dependent
  operations sequential. Use `launch` for child work without a result.
- A non-cancellation child failure in `coroutineScope` cancels siblings. Use `supervisorScope` only when partial
  failure is intentional, and handle each child's failure explicitly. Catching a failed `await` cannot repair an
  already-cancelled regular scope.
- Let callers own request-scoped work. For background work, use a lifecycle-owned scope that is cancelled at shutdown;
  avoid `GlobalScope`, detached jobs, and ad hoc scopes inside service methods.
- Bound in-flight operations with a semaphore; use a worker pool to also bound coroutine count. A dispatcher limit
  alone does not bound concurrent suspending operations. Use `runBlocking` only at a deliberate synchronous
  entry-point boundary.
- Protect shared mutable state with a `Mutex` or explicit confinement.

```kotlin
suspend fun loadDashboard(repository: DashboardRepository): Dashboard = coroutineScope {
    val items = async { repository.recentItems() }
    val statistics = async { repository.statistics() }

    Dashboard(items = items.await(), statistics = statistics.await())
}
```

## Dispatchers and blocking work

- Put blocking I/O behind an I/O dispatcher boundary. Follow the project's dispatcher-injection convention when one
  exists. Use a CPU dispatcher for substantial computation. Leave already-suspending, non-blocking client calls in the
  caller's context.
- `withContext(ioDispatcher)` avoids blocking the caller's thread, but does not make the operation cancellable.
  Use `runInterruptible(ioDispatcher)` only for APIs that correctly support thread interruption.
- Choose timeouts at the operation boundary. A coroutine timeout cannot stop an arbitrary non-cooperative blocking call.

```kotlin
class FileReader(private val ioDispatcher: CoroutineDispatcher) {
    suspend fun read(path: Path): String = withContext(ioDispatcher) {
        Files.readString(path)
    }
}
```

## Cancellation and cleanup

- Let `CancellationException` propagate. Broad catches and `runCatching` can swallow cancellation; rethrow cancellation
  before translating other failures. Recover only from failures with a defined domain policy.
- Check `currentCoroutineContext().ensureActive()` in long CPU loops without suspension points.
- Release resources in `finally` or `use`. Keep `NonCancellable` limited to necessary suspending cleanup,
  not normal work.

```kotlin
suspend fun fetchOptionalNotice(client: NoticeClient): String? = try {
    client.fetch()
} catch (failure: IOException) {
    null
}
```

Here only an I/O failure means an unavailable optional notice; cancellation and other failures propagate.

```kotlin
suspend fun process(resource: SuspendResource) {
    try {
        resource.process()
    } finally {
        withContext(NonCancellable) {
            resource.release()
        }
    }
}
```

## Flow

For `Flow`, `StateFlow`, or `SharedFlow` work, read [`references/flow.md`](references/flow.md).

## Testing

- Prefer Kotest `coroutineTestScope = true` for virtual-time tests, and control time with the `testScheduler`
  extension on the test scope.
- `testScheduler` returns a `kotlinx-coroutines-test` type. Declare that dependency explicitly before using its APIs;
  do not rely on the copy Kotest brings in transitively.
