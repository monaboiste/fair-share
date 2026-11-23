package fairshare.infrastructure.persistence

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupFailure
import fairshare.application.group.GroupId
import fairshare.application.group.GroupSnapshot
import fairshare.application.group.GroupStore
import fairshare.application.group.GroupStoreUnavailable
import fairshare.application.group.GroupStream
import fairshare.application.group.GroupUseCases
import fairshare.domain.group.GroupEvent
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SqliteGroupStoreTest :
    BehaviorSpec({
        given("two instances sharing a durable Group store") {
            then("after both commands load the same revision only one conditional write commits") {
                val url = "jdbc:sqlite:${tempdir().resolve("groups.db")}"
                val firstStore = SqliteGroupStore(url)
                val secondStore = SqliteGroupStore(url)
                val first = GroupUseCases(firstStore)
                val second = GroupUseCases(secondStore)
                val group = first.create("Trip", "PLN")
                val other = second.create("Other trip", "USD")
                second.get(group.id).revision shouldBe group.revision
                first.get(other.id).revision shouldBe other.revision

                val barrier = CyclicBarrier(2)
                val firstWriter = GroupUseCases(firstStore.waitForOtherLoad(barrier))
                val secondWriter = GroupUseCases(secondStore.waitForOtherLoad(barrier))
                val id = group.id
                val revision = group.revision
                val committed = Executors.newFixedThreadPool(2).use { workers ->
                    val eur = workers.submit<GroupSnapshot> { firstWriter.changeCurrency(id, revision, "EUR") }
                    val jpy = workers.submit<GroupSnapshot> { secondWriter.changeCurrency(id, revision, "JPY") }
                    listOf(eur, jpy).mapNotNull { future ->
                        try {
                            future.get(10, TimeUnit.SECONDS)
                        } catch (failure: ExecutionException) {
                            if (failure.cause !is GroupFailure.RevisionMismatch) throw failure
                            null
                        }
                    }.single()
                }

                val firstRead = first.get(group.id)
                val secondRead = second.get(group.id)
                firstRead.revision shouldBe committed.revision
                secondRead.revision shouldBe committed.revision
                secondRead.group.settlementCurrency shouldBe committed.group.settlementCurrency
                val complete = second.changeCurrency(group.id, secondRead.revision, "KWD")
                first.get(group.id).revision shouldBe complete.revision
                first.get(group.id).group.settlementCurrency.code shouldBe "KWD"
                first.get(other.id).revision shouldBe other.revision
                first.get(other.id).group.settlementCurrency.code shouldBe "USD"
                shouldThrow<GroupFailure.RevisionMismatch> { first.changeCurrency(other.id, complete.revision, "JPY") }
            }

            then("a confirmed write failure leaves the previously committed currency and revision intact") {
                val database = tempdir().resolve("groups.db").toURI()
                val writable = GroupUseCases(SqliteGroupStore("jdbc:sqlite:$database"))
                val created = writable.create("Trip", "PLN")
                val readOnly = GroupUseCases(SqliteGroupStore("jdbc:sqlite:$database?mode=ro"))
                readOnly.get(created.id).revision shouldBe created.revision
                shouldThrow<GroupStoreUnavailable> { readOnly.changeCurrency(created.id, created.revision, "EUR") }
                val persisted = writable.get(created.id)
                persisted.revision shouldBe created.revision
                persisted.group.settlementCurrency.code shouldBe "PLN"
            }

            then("creation can only start an empty stream and a configured Group cannot be re-created") {
                val store = SqliteGroupStore("jdbc:sqlite:${tempdir().resolve("groups.db")}")
                val created = GroupUseCases(store).create("Trip", "PLN")
                val event = GroupEvent.Created(checkNotNull(GroupName.parse("Other")), created.group.settlementCurrency)
                store.append(created.id, null, event) shouldBe GroupAppend.RevisionMismatch
                store.load(created.id)?.revision shouldBe created.revision
                shouldThrow<IllegalArgumentException> { store.append(created.id, created.revision, event) }

                val absent = GroupId("absent")
                store.append(
                    absent,
                    created.revision,
                    GroupEvent.CurrencyChanged(checkNotNull(SettlementCurrency.parse("EUR"))),
                ) shouldBe GroupAppend.RevisionMismatch
                store.load(absent) shouldBe null
            }
        }
    })

private fun GroupStore.waitForOtherLoad(barrier: CyclicBarrier): GroupStore = object : GroupStore by this {
    override fun load(id: GroupId): GroupStream {
        val stream = this@waitForOtherLoad.load(id).shouldNotBeNull()
        barrier.await(5, TimeUnit.SECONDS)
        return stream
    }
}
