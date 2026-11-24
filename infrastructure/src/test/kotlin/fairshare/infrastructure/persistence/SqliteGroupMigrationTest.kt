package fairshare.infrastructure.persistence

import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupStoreUnavailable
import fairshare.domain.group.Group
import fairshare.domain.group.GroupId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.sql.Connection
import java.sql.DriverManager

class SqliteGroupMigrationTest :
    ExpectSpec({
        expect("legacy migration preserves persisted Group revisions and event order") {
            val url = "jdbc:sqlite:${tempdir().resolve("legacy.db")}"
            database(url) { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(LEGACY_TABLE)
                    statement.executeUpdate(
                        """
                        INSERT INTO group_events VALUES
                            ('trip', 3, 'rev-third', 'settlement-currency-changed', NULL, 'KWD'),
                            ('trip', 1, 'rev-first', 'group-created', 'Weekend trip', 'PLN'),
                            ('trip', 2, 'rev-second', 'settlement-currency-changed', NULL, 'EUR')
                        """.trimIndent(),
                    )
                }
            }

            val stream = checkNotNull(store(url).load(GroupId("trip")))
            stream.revision shouldBe GroupRevision("rev-third")
            stream.events.size shouldBe 3
            Group.replay(stream.events).settlementCurrency.code shouldBe "KWD"
            store(url).load(GroupId("trip")) shouldBe stream
            database(url) { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA user_version").use { versions ->
                        versions.next() shouldBe true
                        versions.getInt(1) shouldBe 1
                    }
                    statement.executeQuery("SELECT revision, format_version FROM group_events ORDER BY position")
                        .use { rows ->
                            buildList { while (rows.next()) add(rows.getString(1) to rows.getInt(2)) } shouldBe
                                listOf("rev-first" to 1, "rev-second" to 1, "rev-third" to 1)
                        }
                }
            }
        }

        expect("a current schema can start while another connection holds a write reservation") {
            val url = "jdbc:sqlite:${tempdir().resolve("startup.db")}"
            store(url)
            database(url) { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("BEGIN IMMEDIATE")
                    SqliteGroupStore(SqliteGroupStoreSettings(jdbcUrl = url, busyTimeoutMillis = 0))
                    statement.execute("ROLLBACK")
                }
            }
        }

        expect("a newer schema is rejected on startup and left alone") {
            val url = "jdbc:sqlite:${tempdir().resolve("newer.db")}"
            database(url) { connection ->
                connection.createStatement().use { it.execute("PRAGMA user_version = 2") }
            }
            shouldThrow<IllegalStateException> { store(url) }.message shouldBe "Unsupported Group schema version: 2"
            database(url, ::schemaVersion) shouldBe 2
        }

        expect("a current version marker cannot make an incompatible table usable") {
            val url = "jdbc:sqlite:${tempdir().resolve("incompatible.db")}"
            database(url) { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(LEGACY_TABLE)
                    statement.execute("PRAGMA user_version = 1")
                }
            }
            shouldThrow<GroupStoreUnavailable> { store(url) }
            database(url, ::schemaVersion) shouldBe 1
        }

        expect("a failed legacy migration leaves both schema version and existing data intact") {
            val url = "jdbc:sqlite:${tempdir().resolve("broken.db")}"
            database(url) { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE group_events (group_id TEXT)")
                    statement.executeUpdate("INSERT INTO group_events VALUES ('keep')")
                }
            }
            shouldThrow<GroupStoreUnavailable> { store(url) }
            database(url) { connection ->
                schemaVersion(connection) shouldBe 0
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT group_id FROM group_events").use { rows ->
                        rows.next() shouldBe true
                        rows.getString(1) shouldBe "keep"
                    }
                    statement.executeQuery(
                        "SELECT name FROM sqlite_schema WHERE name = 'group_events_v1'",
                    ).use { rows ->
                        rows.next() shouldBe false
                    }
                }
            }
        }

        expect("future event kinds and unsupported encoding versions are never skipped or misread as current facts") {
            val url = "jdbc:sqlite:${tempdir().resolve("future-events.db")}"
            val writer = store(url)
            database(url) { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        INSERT INTO group_events VALUES
                            ('type', 1, 'rev1', 'group-created', 1, 'Trip', 'PLN'),
                            ('type', 2, 'rev2', 'unknown-currency-kind', 1, NULL, 'EUR'),
                            ('version', 1, 'rev1', 'group-created', 1, 'Trip', 'PLN'),
                            ('version', 2, 'rev2', 'settlement-currency-changed', 2, NULL, 'EUR'),
                            ('fractional', 1, 'rev1', 'group-created', 1.5, 'Trip', 'PLN'),
                            ('missing-name', 1, 'rev1', 'group-created', 1, NULL, 'PLN'),
                            ('blank-name', 1, 'rev1', 'group-created', 1, '', 'PLN'),
                            ('bad-currency', 1, 'rev1', 'group-created', 1, 'Trip', 'ZZZ')
                        """.trimIndent(),
                    )
                }
            }
            shouldThrow<IllegalStateException> { writer.load(GroupId("type")) }
            shouldThrow<IllegalStateException> { writer.load(GroupId("version")) }
            shouldThrow<IllegalStateException> { writer.load(GroupId("fractional")) }
            shouldThrow<IllegalStateException> { writer.load(GroupId("missing-name")) }
                .message shouldBe "Missing Group event value: display_name"
            shouldThrow<IllegalStateException> { writer.load(GroupId("blank-name")) }
                .message shouldBe "Invalid recorded Group name"
            shouldThrow<IllegalStateException> { writer.load(GroupId("bad-currency")) }
                .message shouldBe "Invalid recorded currency"
        }
    })

private fun schemaVersion(connection: Connection): Int = connection.createStatement().use { statement ->
    statement.executeQuery("PRAGMA user_version").use { versions ->
        check(versions.next())
        versions.getInt(1)
    }
}

internal fun <T> database(url: String, query: (Connection) -> T): T = DriverManager.getConnection(url).use(query)

private val LEGACY_TABLE = """
    CREATE TABLE group_events (
        group_id TEXT NOT NULL,
        position INTEGER NOT NULL,
        revision TEXT NOT NULL,
        kind TEXT NOT NULL,
        display_name TEXT,
        currency TEXT NOT NULL,
        PRIMARY KEY (group_id, position)
    )
""".trimIndent()
