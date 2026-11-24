package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupView
import fairshare.domain.group.GroupId
import fairshare.domain.group.SettlementCurrency
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Clock
import java.time.Duration
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SqliteGroupEventStoreTest :
    BehaviorSpec({
        given("two instances sharing a durable Group database") {
            then("only one competing batch commits, visible immediately and after restart without interleaving") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val first = db.eventStore()
                val second = db.eventStore()
                val other = GroupId("other")
                val opened = first.append(trip, null, opening).committedRevision()
                val otherOpened = second.append(other, null, opening).committedRevision()
                val batches = listOf(first to changes("EUR", "JPY"), second to changes("KWD", "USD"))
                val barrier = CyclicBarrier(batches.size)
                val outcomes = Executors.newFixedThreadPool(batches.size).use { workers ->
                    batches.map { (writer, batch) ->
                        workers.submit<Pair<GroupEventBatch, GroupAppend>> {
                            val expected = checkNotNull(writer.load(trip)).revision
                            expected shouldBe opened
                            barrier.await(5, TimeUnit.SECONDS)
                            batch to writer.append(trip, expected, batch)
                        }
                    }.map { it.get(10, TimeUnit.SECONDS) }
                }

                val (batch, outcome) = outcomes.single { it.second is GroupAppend.Committed }
                val revision = outcome.committedRevision()
                outcomes.single { it.first !== batch }.second shouldBe GroupAppend.RevisionMismatch
                revision shouldNotBe opened
                val snapshot = checkNotNull(second.load(trip))
                snapshot.revision shouldBe revision
                snapshot.events.map { it.event } shouldBe opening.events + batch.events
                snapshot.events.map { it.revision } shouldBe listOf(opened, revision, revision)
                db.initialize()
                db.eventStore().load(trip) shouldBe snapshot

                val otherView = GroupView(other, tripName, SettlementCurrency("PLN"), otherOpened)
                db.viewRepository().findById(other) shouldBe otherView
                first.append(other, revision, changes("EUR")) shouldBe GroupAppend.RevisionMismatch
                first.load(other)?.revision shouldBe otherOpened
            }

            then("stable unique ID and clock metadata describe every event in sequence, not timestamp order") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val later = db.eventStore()
                val olderClock = Clock.offset(fixedClock, Duration.ofDays(-1))
                val earlier = db.eventStore(olderClock)
                val created = later.append(trip, null, opening).committedRevision()
                val changed = earlier.append(trip, created, changes("EUR", "JPY")).committedRevision()
                val other = GroupId("other")
                earlier.append(other, null, opening).committedRevision()

                val snapshot = checkNotNull(earlier.load(trip))
                val events = snapshot.events
                events.map { it.sequence } shouldBe listOf(1L, 2L, 3L)
                events.map { it.groupId } shouldBe List(3) { trip }
                events.map { it.revision } shouldBe listOf(created, changed, changed)
                val timestamps = listOf(fixedClock.instant(), olderClock.instant(), olderClock.instant())
                events.map { it.recordedAt } shouldBe timestamps
                val ids = (events + checkNotNull(later.load(other)).events).map { it.eventId }
                ids.distinct().size shouldBe ids.size
                db.eventStore().load(trip) shouldBe snapshot
                val expectedView = GroupView(trip, tripName, SettlementCurrency("JPY"), changed)
                db.viewRepository().findById(trip) shouldBe expectedView
            }

            then("an absent stream opens only once and cannot be updated without creation") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val writer = db.eventStore()
                val absent = GroupId("absent")
                val arbitrary = GroupRevision.random()
                writer.append(absent, arbitrary, changes("EUR")) shouldBe GroupAppend.RevisionMismatch
                writer.load(absent) shouldBe null
                db.viewRepository().findById(absent) shouldBe null
                shouldThrow<IllegalArgumentException> { writer.append(absent, null, changes("EUR")) }

                val batch = GroupEventBatch(opening.events + changes("EUR").events)
                val opened = writer.append(trip, null, batch).committedRevision()
                writer.append(trip, null, opening) shouldBe GroupAppend.RevisionMismatch
                shouldThrow<IllegalArgumentException> { writer.append(trip, opened, opening) }
                writer.load(trip)?.events?.map { it.event } shouldBe batch.events
                val expectedView = GroupView(trip, tripName, SettlementCurrency("EUR"), opened)
                db.viewRepository().findById(trip) shouldBe expectedView
            }
        }
    })
