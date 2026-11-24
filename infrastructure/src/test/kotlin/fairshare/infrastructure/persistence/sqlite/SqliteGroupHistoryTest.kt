package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupStoreUnavailable
import fairshare.domain.group.GroupId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SqliteGroupHistoryTest :
    ExpectSpec({
        context("both read adapters reject corruption instead of returning a partial Group") {
            withData(
                nameFn = { it.first },
                "unknown event kind" to "kind = 'unknown' WHERE position = 2",
                "unsupported event format" to "format_version = 99 WHERE position = 2",
                "blank Group name" to "display_name = ' ' WHERE position = 1",
                "missing Group name" to "display_name = NULL WHERE position = 1",
                "historical invalid currency" to "currency = 'invalid' WHERE position = 2",
                "unexpected payload field" to "display_name = 'Surprise' WHERE position = 2",
                "missing event ID" to "event_id = '' WHERE position = 2",
                "missing revision" to "revision = '' WHERE position = 2",
                "invalid timestamp" to "recorded_at = 'yesterday' WHERE position = 2",
                "duplicate opening event" to "kind = 'group-created', display_name = 'Duplicate' WHERE position = 2",
                "missing opening event" to
                    "kind = 'settlement-currency-changed', display_name = NULL WHERE position = 1",
                "non-contiguous sequence" to "position = 10 WHERE position = 3",
                "reused retired revision" to
                    "revision = (SELECT revision FROM group_events WHERE position = 1) WHERE position = 3",
            ) { (_, corruption) ->
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val store = db.eventStore()
                val opened = store.append(trip, null, opening).committedRevision()
                val changed = store.append(trip, opened, changes("EUR")).committedRevision()
                store.append(trip, changed, changes("JPY")).committedRevision()
                db.execute("UPDATE group_events SET $corruption")

                val loadFailure = shouldThrow<IllegalStateException> { store.load(trip) }
                val viewFailure = shouldThrow<IllegalStateException> { db.viewRepository().findById(trip) }
                loadFailure.message shouldContain "Invalid Group trip history at event"
                viewFailure.message shouldBe loadFailure.message
            }
        }

        expect("initialization refuses a legacy table without rewriting the database file") {
            val db = SqliteTestFixture(tempdir())
            db.execute(
                """
            CREATE TABLE group_events (
                group_id TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY (group_id, position)
            )
                """.trimIndent(),
            )
            val original = db.file.readBytes()

            shouldThrow<GroupStoreUnavailable> { db.initialize() }
            db.file.readBytes().toList() shouldBe original.toList()
        }

        expect("initialization refuses a same-column table with case-insensitive Group IDs without rewriting it") {
            val db = SqliteTestFixture(tempdir())
            db.execute(
                """
            CREATE TABLE group_events (
                group_id TEXT COLLATE NOCASE NOT NULL,
                position INTEGER NOT NULL,
                event_id TEXT NOT NULL UNIQUE,
                revision TEXT NOT NULL,
                recorded_at TEXT NOT NULL,
                kind TEXT NOT NULL,
                format_version INTEGER NOT NULL,
                display_name TEXT,
                currency TEXT NOT NULL,
                PRIMARY KEY (group_id, position)
            ) STRICT
                """.trimIndent(),
            )
            val original = db.file.readBytes()

            shouldThrow<GroupStoreUnavailable> { db.initialize() }
            db.file.readBytes().toList() shouldBe original.toList()
        }

        expect("initialization does not replace an incompatible view using the Group table name") {
            val db = SqliteTestFixture(tempdir())
            db.execute("CREATE VIEW group_events AS SELECT 1")
            val original = db.file.readBytes()

            shouldThrow<GroupStoreUnavailable> { db.initialize() }
            db.file.readBytes().toList() shouldBe original.toList()
        }

        expect("using either adapter before schema initialization fails rather than reporting an absent Group") {
            val db = SqliteTestFixture(tempdir())
            val absent = GroupId("absent")
            shouldThrow<GroupStoreUnavailable> { db.eventStore().load(absent) }
            shouldThrow<GroupStoreUnavailable> { db.viewRepository().findById(absent) }
        }
    })
