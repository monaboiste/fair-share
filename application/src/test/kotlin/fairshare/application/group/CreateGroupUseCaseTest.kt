package fairshare.application.group

import fairshare.domain.group.events.GroupCreated
import fairshare.domain.group.exception.InvalidGroupNameException
import fairshare.domain.group.exception.InvalidSettlementCurrencyException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CreateGroupUseCaseTest :
    BehaviorSpec({
        given("an empty Group event store") {
            then("invalid creation settings fail domain validation without attempting to write") {
                val store = RecordingGroupEventStore()
                val create = CreateGroupUseCase(store)

                shouldThrow<InvalidGroupNameException> { create.execute(" ", "PLN") }
                shouldThrow<InvalidSettlementCurrencyException> { create.execute("Name", "ZZZ") }
                store.appendCount shouldBe 0
            }

            then("valid settings produce one opening event and return the committed revision") {
                val store = RecordingGroupEventStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")

                val recorded = store.streams.getValue(created.id).events.single()
                recorded.event shouldBe GroupCreated(created.name, created.settlementCurrency)
                recorded.revision shouldBe created.revision
                created.name.value shouldBe "Trip"
                created.settlementCurrency.code shouldBe "PLN"
                created.revision.value shouldBe "revision-1"
            }

            then("a rejected creation is a failure, not a version mismatch or retried write") {
                val store = RecordingGroupEventStore().apply { rejectNextAppend = true }
                shouldThrow<IllegalStateException> { CreateGroupUseCase(store).execute("Trip", "PLN") }
                store.appendCount shouldBe 1
            }
        }
    })
