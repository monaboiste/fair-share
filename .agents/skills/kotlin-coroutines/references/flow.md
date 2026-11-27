# Flow, StateFlow, and SharedFlow

## Flow pipelines

- A cold flow runs its upstream work for each collection. Return an existing flow with operators instead of wrapping it
  in `flow { collect { emit(it) } }`. Use `flow` for production and `callbackFlow` with `awaitClose` for callback APIs.
- `flowOn` changes the upstream context, not the collector's context. Keep emissions in a `flow` builder's context; use
  `flowOn` rather than emitting from a nested `withContext`.
- `map` transforms values; `mapLatest` cancels an obsolete transformation; `flatMapLatest` switches to a new inner flow.
  Use latest/conflating operators only when dropping obsolete work or intermediate values is acceptable.
- `combine` uses each upstream's latest value after all have emitted; `zip` pairs corresponding emissions.
- `catch` handles upstream failures, not downstream collector errors or cancellation. A fallback emission ends that
  upstream collection; it does not resume observation. Model failure explicitly when an empty result would hide it.
- Retry only transient failures, with a bounded attempt count and backoff. Retrying recollects upstream and may repeat
  side effects, so verify idempotency. Place recovery inside an inner flow when a failed request must not end the outer
  stream.
- Use `buffer` only with a deliberate capacity/overflow policy. Use `conflate` only when consumers need the latest value
  rather than every emission.

```kotlin
fun observeNames(repository: ParticipantRepository): Flow<List<String>> =
    repository.observeParticipants()
        .map { participants -> participants.map { it.name } }
        .distinctUntilChanged()
```

## Shared state and streams

- Use `StateFlow` for current state: it requires an initial value, suppresses equal updates, and slow collectors may
  skip intermediate states. Keep mutable flows private and expose `asStateFlow()` or `asSharedFlow()`.
- Use immutable state snapshots and atomic `update` for read-modify-write changes. Its lambda may run more than once, so
  keep it free of side effects.
- Use `SharedFlow` for broadcast with explicit replay and overflow semantics. With no subscribers, only replay values
  survive; extra buffer capacity does not retain events. It is not a durable or exactly-once event queue.
- Use `stateIn`/`shareIn` only when collectors should share upstream work. Supply an owner scope and choose a start/stop
  policy from the consumer lifecycle; handle upstream failures before sharing.

```kotlin
class ParticipantRegistry {
    private val mutableParticipants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = mutableParticipants.asStateFlow()

    fun add(participant: Participant) {
        mutableParticipants.update { current -> current + participant }
    }
}
```
