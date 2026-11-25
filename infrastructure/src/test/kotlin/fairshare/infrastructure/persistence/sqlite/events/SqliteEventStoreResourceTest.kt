package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.springframework.dao.DataAccessException
import java.sql.SQLException

class SqliteEventStoreResourceTest :
    ExpectSpec({
        val streamId = StreamId("tracked-stream")

        expect("each successful or mismatched append uses one physical connection and closes all JDBC resources") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val resources = TestJdbcResources(database.dataSource)
            val store = testEventStore(resources)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("next"))

            store.append(streamId, StreamRevision.INITIAL, listOf(created, renamed)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            resources.openedCount shouldBe 1
            resources.allClosed shouldBe true

            store.append(streamId, StreamRevision.INITIAL, listOf(created)) shouldBe AppendResult.RevisionMismatch
            resources.openedCount shouldBe 2
            resources.allClosed shouldBe true

            testEventStore(database.newDataSource()).load(streamId)?.events shouldBe
                listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))
        }

        expect("a failed event insert closes all JDBC resources and its only physical connection") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val resources = TestJdbcResources(database.dataSource)
            val store = testEventStore(resources)
            val duplicate = pending(TestCreated("duplicate"))

            shouldThrow<DataAccessException> {
                store.append(streamId, StreamRevision.INITIAL, listOf(duplicate, duplicate))
            }

            resources.openedCount shouldBe 1
            resources.allClosed shouldBe true
            testEventStore(database.newDataSource()).load(streamId) shouldBe null
        }

        expect("schema initialization and loads close all JDBC resources even when a load cannot decode history") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val resources = TestJdbcResources(database.dataSource)
            val store = testEventStore(resources)
            val created = pending(TestCreated("original"))

            SqliteEventSchema.initialize(resources)
            resources.allClosed shouldBe true

            testEventStore(database.dataSource).append(streamId, StreamRevision.INITIAL, listOf(created))
            store.load(streamId)?.events shouldBe listOf(created.envelope(streamId, 1, 1))
            resources.allClosed shouldBe true

            database.executeSql("UPDATE event SET payload = '{'")
            shouldThrow<InvalidEventHistoryException> { store.load(streamId) }
            resources.allClosed shouldBe true
        }

        expect("a JDBC close exception after successful commit does not change the append result or persisted batch") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val resources = TestJdbcResources(
                database.dataSource,
                afterCloseFailure = SQLException("Simulated cleanup failure after closing the connection"),
            )
            val store = testEventStore(resources)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("next"))

            store.append(streamId, StreamRevision.INITIAL, listOf(created, renamed)) shouldBe
                AppendResult.Committed(StreamRevision(1))

            resources.openedCount shouldBe 1
            resources.allClosed shouldBe true
            testEventStore(database.newDataSource()).load(streamId)?.events shouldBe
                listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))
        }
    })
