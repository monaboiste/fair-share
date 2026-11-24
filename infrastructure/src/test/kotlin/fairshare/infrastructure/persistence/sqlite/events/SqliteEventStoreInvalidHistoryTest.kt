package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe

class SqliteEventStoreInvalidHistoryTest :
    ExpectSpec({
        val streamId = StreamId("damaged-stream")

        val corruptions = mapOf(
            "unsupported event type" to listOf("UPDATE event SET event_type = 'test.unknown' WHERE sequence = 2"),
            "unsupported event version" to listOf("UPDATE event SET event_version = 2 WHERE sequence = 2"),
            "event version overflowing an integer" to listOf(
                "PRAGMA ignore_check_constraints = ON",
                "UPDATE event SET event_version = 4294967297 WHERE sequence = 2",
            ),
            "malformed JSON payload" to listOf("UPDATE event SET payload = '{' WHERE sequence = 2"),
            "JSON payload without a required field" to listOf("UPDATE event SET payload = '{}' WHERE sequence = 2"),
            "invalid event UUID" to listOf("UPDATE event SET event_id = 'not-a-uuid' WHERE sequence = 2"),
            "noncanonical event UUID" to listOf("UPDATE event SET event_id = '1-1-1-1-1' WHERE sequence = 2"),
            "invalid recorded instant" to listOf("UPDATE event SET recorded_at = 'not-an-instant' WHERE sequence = 2"),
            "header revision ahead of the last event" to listOf("UPDATE event_stream SET revision = 3"),
            "header sequence ahead of the last event" to listOf("UPDATE event_stream SET last_sequence = 5"),
            "header with no events" to listOf("DELETE FROM event"),
            "events with no header" to listOf("PRAGMA foreign_keys = OFF", "DELETE FROM event_stream"),
            "gap in the event sequence" to listOf("DELETE FROM event WHERE sequence = 2"),
            "gap between batch revisions" to listOf(
                "UPDATE event SET revision = 3 WHERE sequence >= 3",
                "UPDATE event_stream SET revision = 3",
            ),
            "regression in batch revisions" to listOf(
                "UPDATE event SET revision = 1 WHERE sequence = 4",
                "UPDATE event_stream SET revision = 1",
            ),
        )
        corruptions.forEach { (description, commands) ->
            expect("loading rejects $description instead of returning a partial or missing history") {
                val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
                database.store.append(
                    streamId,
                    StreamRevision.INITIAL,
                    listOf(pending(TestCreated("original")), pending(TestRenamed("second"))),
                ) shouldBe AppendResult.Committed(StreamRevision(1))
                database.store.append(
                    streamId,
                    StreamRevision(1),
                    listOf(pending(TestRenamed("third")), pending(TestRenamed("fourth"))),
                ) shouldBe AppendResult.Committed(StreamRevision(2))
                database.executeSql(*commands.toTypedArray())

                shouldThrow<InvalidEventHistoryException> { database.store.load(streamId) }
            }
        }
    })
