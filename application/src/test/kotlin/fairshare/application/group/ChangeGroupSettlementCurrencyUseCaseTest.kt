package fairshare.application.group

import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupSettlementCurrencyChanged
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ChangeGroupSettlementCurrencyUseCaseTest :
    BehaviorSpec({
        given("an addressable Group with a current revision") {
            then("a valid change records one event at that revision and returns the committed revision") {
                val store = RecordingGroupStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val changed = change.execute(created.id, GroupRevisionPrecondition(created.revision), "JPY")

                changed.id shouldBe created.id
                changed.revision shouldBe GroupRevision("revision-2")
                changed.group.name shouldBe created.group.name
                changed.group.settlementCurrency.code shouldBe "JPY"
                store.streams.getValue(created.id).events.last() shouldBe
                    GroupSettlementCurrencyChanged(changed.group.settlementCurrency)
                store.appendCount shouldBe 2
            }

            then("missing Groups and stale revisions take priority over invalid settings and do not write") {
                val store = RecordingGroupStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val current = GroupRevisionPrecondition(created.revision)
                val stale = GroupRevisionPrecondition(GroupRevision("stale"))

                shouldThrow<GroupFailure.NotFound> { change.execute(GroupId("missing"), current, "eur") }
                shouldThrow<GroupFailure.RevisionMismatch> { change.execute(created.id, stale, "eur") }
                shouldThrow<GroupFailure.InvalidGroup> { change.execute(created.id, current, "eur") }
                    .violations shouldBe listOf(GroupInputViolation.INVALID_SETTLEMENT_CURRENCY)
                store.appendCount shouldBe 1
            }

            then("a revision mismatch at append is returned without repeating the command") {
                val store = RecordingGroupStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")
                store.rejectNextAppend = true
                val change = ChangeGroupSettlementCurrencyUseCase(store)
                val current = GroupRevisionPrecondition(created.revision)
                shouldThrow<GroupFailure.RevisionMismatch> { change.execute(created.id, current, "EUR") }
                store.appendCount shouldBe 2
                val loaded = GetGroupUseCase(store).execute(created.id)
                loaded.revision shouldBe created.revision
                loaded.group.settlementCurrency.code shouldBe "PLN"
            }
        }
    })
