package fairshare.application.group

import fairshare.domain.group.GroupEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class GroupUseCasesTest :
    BehaviorSpec({
        given("a Group configuration use case") {
            then("invalid creation settings do not attempt an append") {
                val store = RecordingGroupStore()
                val groups = GroupUseCases(store)
                val blank = GroupInputViolation.BLANK_DISPLAY_NAME
                val currency = GroupInputViolation.INVALID_SETTLEMENT_CURRENCY

                val invalidName = shouldThrow<GroupFailure.InvalidGroup> { groups.create(" ", "PLN") }
                val invalidCurrency = shouldThrow<GroupFailure.InvalidGroup> { groups.create("Name", "ZZZ") }
                val both = shouldThrow<GroupFailure.InvalidGroup> { groups.create("", "eur") }
                invalidName.violations shouldBe listOf(blank)
                invalidCurrency.violations shouldBe listOf(currency)
                both.violations shouldBe listOf(blank, currency)
                store.appendCount shouldBe 0
            }

            then("configuration is visible at the revision returned by the store") {
                val store = RecordingGroupStore()
                val groups = GroupUseCases(store)
                val created = groups.create("Trip", "PLN")
                created.group.name.value shouldBe "Trip"
                created.group.settlementCurrency.code shouldBe "PLN"
                created.revision.value shouldBe "revision-1"

                val changed = groups.changeCurrency(created.id, created.revision, "JPY")
                val read = groups.get(created.id)
                read.id shouldBe changed.id
                read.id shouldBe created.id
                read.revision shouldBe changed.revision
                read.revision.value shouldBe "revision-2"
                read.group.settlementCurrency.code shouldBe "JPY"
                changed.group.settlementCurrency.code shouldBe "JPY"
                store.appendCount shouldBe 2
            }

            then("missing Groups, stale revisions and invalid settings do not attempt an update") {
                val store = RecordingGroupStore()
                val groups = GroupUseCases(store)
                val created = groups.create("Trip", "PLN")
                val missing = GroupId("missing")

                shouldThrow<GroupFailure.NotFound> { groups.get(missing) }
                val current = created.revision
                shouldThrow<GroupFailure.NotFound> { groups.changeCurrency(missing, current, "EUR") }
                val stale = GroupRevision("stale")
                val id = created.id
                shouldThrow<GroupFailure.RevisionMismatch> { groups.changeCurrency(id, stale, "eur") }
                val invalid = shouldThrow<GroupFailure.InvalidGroup> {
                    groups.changeCurrency(created.id, created.revision, "eur")
                }
                invalid.violations shouldBe listOf(GroupInputViolation.INVALID_SETTLEMENT_CURRENCY)
                store.appendCount shouldBe 1
            }

            then("a revision mismatch at append is returned without repeating the command") {
                val store = RecordingGroupStore()
                val groups = GroupUseCases(store)
                val created = groups.create("Trip", "PLN")
                store.rejectNextAppend = true
                val id = created.id
                val current = created.revision
                shouldThrow<GroupFailure.RevisionMismatch> { groups.changeCurrency(id, current, "EUR") }
                store.appendCount shouldBe 2
                groups.get(created.id).revision shouldBe created.revision
                groups.get(created.id).group.settlementCurrency.code shouldBe "PLN"
            }

            then("a rejected creation is a failure, not a version mismatch or retried write") {
                val store = RecordingGroupStore().apply { rejectNextAppend = true }
                shouldThrow<IllegalStateException> { GroupUseCases(store).create("Trip", "PLN") }
                store.appendCount shouldBe 1
            }
        }
    })

private class RecordingGroupStore : GroupStore {
    private val streams = mutableMapOf<GroupId, GroupStream>()
    var appendCount = 0
        private set
    var rejectNextAppend = false

    override fun load(id: GroupId): GroupStream? = streams[id]

    override fun append(id: GroupId, expected: GroupRevision?, event: GroupEvent): GroupAppend {
        appendCount++
        if (rejectNextAppend) {
            rejectNextAppend = false
            return GroupAppend.RevisionMismatch
        }
        val stream = streams[id]
        expected shouldBe stream?.revision
        val revision = GroupRevision("revision-$appendCount")
        streams[id] = GroupStream(revision, stream?.events.orEmpty() + event)
        return GroupAppend.Committed(revision)
    }
}
