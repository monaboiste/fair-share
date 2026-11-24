package fairshare.application.group

import fairshare.domain.group.GroupId
import fairshare.domain.group.events.GroupSettlementCurrencyChanged
import fairshare.domain.group.exception.InvalidSettlementCurrencyException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ChangeGroupSettlementCurrencyUseCaseTest :
    BehaviorSpec({
        given("an addressable Group with a current revision") {
            then("a valid change records one event at that revision and returns the committed view") {
                val store = RecordingGroupEventStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                val original = store.streams.getValue(created.id)
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val changed = change.execute(created.id, GroupRevisionPrecondition(created.revision), "JPY")

                changed.id shouldBe created.id
                changed.revision shouldBe GroupRevision("revision-2")
                changed.name shouldBe created.name
                changed.settlementCurrency.code shouldBe "JPY"
                val committed = store.streams.getValue(created.id)
                committed.events.dropLast(1) shouldBe original.events
                val appended = committed.events.last()
                appended.sequence shouldBe 2L
                appended.event shouldBe GroupSettlementCurrencyChanged(changed.settlementCurrency)
                store.appendCount shouldBe 2
            }

            then("missing Groups and stale revisions take priority over invalid settings and do not write") {
                val store = RecordingGroupEventStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val current = GroupRevisionPrecondition(created.revision)
                val stale = GroupRevisionPrecondition(GroupRevision("stale"))

                shouldThrow<GroupFailure.NotFound> { change.execute(GroupId("missing"), current, "eur") }
                shouldThrow<GroupFailure.RevisionMismatch> { change.execute(created.id, stale, "eur") }
                shouldThrow<InvalidSettlementCurrencyException> { change.execute(created.id, current, "eur") }
                store.appendCount shouldBe 1
            }

            then("a matching revision cannot admit an update on malformed history, even with invalid caller input") {
                val store = RecordingGroupEventStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                store.streams[created.id] = GroupStream(created.revision, emptyList())
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val stale = GroupRevisionPrecondition(GroupRevision("stale"))
                val current = GroupRevisionPrecondition(created.revision)

                shouldThrow<GroupFailure.RevisionMismatch> { change.execute(created.id, stale, "eur") }
                shouldThrow<IllegalStateException> { change.execute(created.id, current, "eur") }
                store.appendCount shouldBe 1
            }

            then("a revision mismatch at append is returned without reloading or repeating the command") {
                val store = RecordingGroupEventStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                val original = store.streams.getValue(created.id)
                store.rejectNextAppend = true
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val current = GroupRevisionPrecondition(created.revision)

                shouldThrow<GroupFailure.RevisionMismatch> { change.execute(created.id, current, "EUR") }
                store.appendCount shouldBe 2
                store.loadCount shouldBe 1
                store.streams.getValue(created.id) shouldBe original
            }

            then("an unknown append outcome is reported without assuming rollback or retrying") {
                val store = RecordingGroupEventStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                val failure = GroupAppendOutcomeUnknown(IllegalStateException("commit acknowledgement lost"))
                store.appendFailure = failure
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val current = GroupRevisionPrecondition(created.revision)

                shouldThrow<GroupAppendOutcomeUnknown> { change.execute(created.id, current, "EUR") } shouldBe failure
                store.appendCount shouldBe 2
                store.loadCount shouldBe 1
            }
        }
    })
