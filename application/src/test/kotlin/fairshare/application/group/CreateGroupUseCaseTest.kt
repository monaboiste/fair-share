package fairshare.application.group

import fairshare.domain.group.GroupCreated
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CreateGroupUseCaseTest :
    BehaviorSpec({
        given("an empty Group store") {
            then("invalid creation settings report all violations without attempting to write") {
                val store = RecordingGroupStore()
                val create = CreateGroupUseCase(store)
                val blank = GroupInputViolation.BLANK_DISPLAY_NAME
                val currency = GroupInputViolation.INVALID_SETTLEMENT_CURRENCY

                shouldThrow<GroupFailure.InvalidGroup> { create.execute(" ", "PLN") }.violations shouldBe listOf(blank)
                shouldThrow<GroupFailure.InvalidGroup> { create.execute("Name", "ZZZ") }.violations shouldBe
                    listOf(currency)
                shouldThrow<GroupFailure.InvalidGroup> { create.execute("", "eur") }.violations shouldBe
                    listOf(blank, currency)
                store.appendCount shouldBe 0
            }

            then("valid settings produce one opening event and the committed revision") {
                val store = RecordingGroupStore()
                val created = CreateGroupUseCase(store).execute("Trip", "PLN")

                val event = store.streams.getValue(created.id).events.single()
                event shouldBe GroupCreated(created.group.name, created.group.settlementCurrency)
                created.group.name.value shouldBe "Trip"
                created.group.settlementCurrency.code shouldBe "PLN"
                created.revision.value shouldBe "revision-1"
            }

            then("a rejected creation is a failure, not a version mismatch or retried write") {
                val store = RecordingGroupStore().apply { rejectNextAppend = true }
                shouldThrow<IllegalStateException> { CreateGroupUseCase(store).execute("Trip", "PLN") }
                store.appendCount shouldBe 1
            }
        }
    })
