---
name: tdd
description: >
  TDD for Kotlin with Kotest. Use when adding behavior, fixing bugs,
  or refactoring production code.
---

# TDD

## Test strategy

- Default to **behavioral slice tests** through a stable application boundary with real collaborators. Assert observable
  outcomes.
- Use focused **unit tests** for algorithms or pure logic whose edge cases are clearer in isolation.
- Do not widen a test to the application or framework context unless the behavior depends on framework wiring or
  integration.
- Give scenarios and blocks descriptions of domain intent: "removing a participant retires their identifier", rather
  than "removeParticipant returns success".
- Use `withData` for named data-driven examples and `checkAll` / `forAll` for meaningful invariants.

## RED → GREEN → REFACTOR

1. **RED:** Write one test for the next behavior. For a bug, reproduce it. Run the narrowest relevant test and confirm
   the failure is caused by the missing behavior, not by an unrelated test, setup, or infrastructure failure. Keep the
   code compiling where practical so the test fails on its assertion.
2. **GREEN:** Implement only enough production behavior to pass. Rerun the test and confirm it passes.
3. **REFACTOR:** Improve production and test code without changing behavior. Rerun the relevant tests and keep them
   green.
4. Repeat for the next behavior.

For behavior-preserving refactors, start with a green baseline and add missing characterization tests before changing
production code.

## Test structure

Default to **BehaviorSpec** for behavioral slices. Give every `given`, `when`, and `then` a meaningful description.
Kotlin requires backticks around `when`.

```kotlin
class ParticipantRemovalTest : BehaviorSpec({
    given("an active participant") {
        val fixture = participantFixture()
        val participant = fixture.application.addParticipant("Alice")

        `when`("the participant is removed") {
            fixture.application.removeParticipant(participant.id)

            then("their identifier is retired and cannot be reused") {
                fixture.application.activeParticipants() shouldNotContain participant
                shouldThrow<RetiredIdentifierException> {
                    fixture.application.addParticipant("Alice", participant.id)
                }
            }
        }
    }
})
```

Keep mutable state local to one scenario path: setup in `given`, action in `when`, assertions in `then`. Do not reuse
mutated fixtures across sibling scenarios.

For compact tests where action and assertion naturally form one expression, use **ExpectSpec**:

```kotlin
class SortingTest : ExpectSpec({
    expect("sorting preserves duplicate values in ascending order") {
        listOf(3, 1, 3).sorted() shouldBe listOf(1, 3, 3)
    }
})
```

## Verification

During each cycle, run the relevant module's test task filtered to the spec:

```bash
./gradlew :<module>:test --tests '<fully.qualified.Spec>'
```

Before completing the change, run:

```bash
./gradlew coverageCheck
```
