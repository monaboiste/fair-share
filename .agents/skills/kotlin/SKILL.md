---
name: kotlin
description: >
  Idiomatic Kotlin implementation patterns. Use when implementing or refactoring Kotlin code,
  designing Kotlin types or APIs, or working with state, nullability, failures, and collections.
---

# Kotlin

Follow nearby code and existing abstractions.

## Types and state

- Default to `val` and narrowly scoped state. Use `data class` for value-like data, ordinary classes for services and
  identity-bearing objects.
- Expose read-only collections and keep mutable backing state private. `val` and `List` are not deep immutability;
  `copy()` is shallow. Snapshot mutable inputs when ownership crosses a boundary.
- Use value classes when mixing primitive identifiers would be a real error. Check framework/serialization support;
  value classes can be boxed at generic, nullable, and interface boundaries.
- Keep invariants in constructors or domain operations so every creation/update path enforces them.

```kotlin
@JvmInline
value class ParticipantId(val value: String) {
    init {
        require(value.isNotBlank()) { "Participant identifier must not be blank" }
    }
}

data class Participant(val id: ParticipantId, val name: String) {
    init {
        require(name.isNotBlank()) { "Participant name must not be blank" }
    }
}
```

## Nullability and failures

- Make nullability express actual absence. Use safe calls, early returns, and Elvis expressions instead of `!!`.
  Supply a fallback only when the domain defines one; missing required data should remain a visible failure.
- Normalize Java platform types at the boundary into explicit nullable or non-nullable Kotlin types.
- Use nullable results for simple absence and sealed outcomes when callers must distinguish domain failures.
  Use `require` for programmer-supplied preconditions and `check` for internal state invariants.
- Do not use `Result` as a substitute for a domain error model. Avoid `runCatching` where cancellation or selective
  exception handling matters: it catches every `Throwable`.
- Catch specific failures only where recovery or boundary translation is meaningful. Preserve the cause when wrapping
  exceptions; release closeable resources with `use`.
- Match sealed types with exhaustive `when` expressions so adding a case exposes affected callers.

```kotlin
sealed interface RemovalOutcome {
    data object Removed : RemovalOutcome
    data object NotFound : RemovalOutcome
    data object AlreadyRemoved : RemovalOutcome
}

fun RemovalOutcome.message(): String = when (this) {
    RemovalOutcome.Removed -> "Participant removed"
    RemovalOutcome.NotFound -> "Participant not found"
    RemovalOutcome.AlreadyRemoved -> "Participant was already removed"
}
```

## Readable APIs

- Give declarations the narrowest useful visibility: `private` within a file or class, `internal` within a module.
- Prefer expression bodies for short functions; use block bodies and named intermediate values when they make the
  control flow clearer. Give public API return types explicitly.
- Use named arguments where adjacent values or booleans would obscure intent. Prefer a domain type over a cluster of
  flags when states have distinct meaning.
- Keep extensions domain-specific and as local as possible. Prefer composition; use interface delegation (`by`)
  when forwarding an existing contract.
- Use scope functions for one clear purpose: `let` transforms a value, `apply` configures an object, `also` adds a
  side effect. Prefer direct safe-call chains or local variables over nested scope functions and ambiguous receivers.

## Collections

- Choose the operation that states intent: `map`, `mapNotNull`, `filter`, `any`, `firstOrNull`, `groupBy`.
  Use a straightforward loop when a chain obscures stateful logic.
- Start with ordinary collection operations. Use sequences for meaningful short-circuiting or large pipelines;
  they add overhead and are not automatically faster.
- `associateBy` overwrites duplicate keys. Validate uniqueness when losing an entry would violate the domain.

```kotlin
fun participantNames(participants: List<Participant>, limit: Int): List<String> {
    require(limit >= 0) { "Limit must be non-negative" }
    return participants.asSequence()
        .map { it.name }
        .distinct()
        .take(limit)
        .toList()
}
```
