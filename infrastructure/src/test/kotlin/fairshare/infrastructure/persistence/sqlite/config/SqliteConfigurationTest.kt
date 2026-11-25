package fairshare.infrastructure.persistence.sqlite.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.sqlite.SQLiteConfig
import java.sql.Statement

class SqliteConfigurationTest :
    ExpectSpec({
        val synchronousModes = mapOf(SQLiteConfig.SynchronousMode.FULL to "2", SQLiteConfig.SynchronousMode.OFF to "0")

        synchronousModes.forEach { (synchronous, pragmaValue) ->
            expect("each SQLite connection uses WAL with foreign keys, a busy timeout and synchronous $synchronous") {
                val databaseFile = tempdir().resolve("configured.db")
                val properties = SqliteProperties(
                    jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}",
                    busyTimeoutMillis = 1234,
                    synchronous = synchronous,
                )
                val dataSource = SqliteConfiguration().dataSource(properties)

                repeat(2) {
                    dataSource.connection.use { connection ->
                        connection.createStatement().use { statement ->
                            statement.readPragma("journal_mode") shouldBe "wal"
                            statement.readPragma("foreign_keys") shouldBe "1"
                            statement.readPragma("busy_timeout") shouldBe "1234"
                            statement.readPragma("synchronous") shouldBe pragmaValue
                        }
                    }
                }
                databaseFile.isFile shouldBe true
            }
        }

        expect("explicit DELETE switches a WAL database to rollback journaling on every connection") {
            val databaseFile = tempdir().resolve("configured.db")
            val properties = SqliteProperties(
                jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}",
                busyTimeoutMillis = 1234,
                synchronous = SQLiteConfig.SynchronousMode.FULL,
            )
            val configuration = SqliteConfiguration()
            configuration.dataSource(properties).connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.readPragma("journal_mode") shouldBe "wal"
                }
            }

            val dataSource = configuration.dataSource(properties.copy(journalMode = SQLiteConfig.JournalMode.DELETE))
            repeat(2) {
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.readPragma("journal_mode") shouldBe "delete"
                    }
                }
            }
        }

        val unsupportedJournalModes = listOf(
            SQLiteConfig.JournalMode.TRUNCATE,
            SQLiteConfig.JournalMode.PERSIST,
            SQLiteConfig.JournalMode.MEMORY,
            SQLiteConfig.JournalMode.OFF,
        )
        unsupportedJournalModes.forEach { journalMode ->
            expect("SQLite configuration rejects $journalMode journaling") {
                shouldThrow<IllegalArgumentException> {
                    SqliteProperties(
                        jdbcUrl = "jdbc:sqlite:configured.db",
                        busyTimeoutMillis = 1234,
                        synchronous = SQLiteConfig.SynchronousMode.FULL,
                        journalMode = journalMode,
                    )
                }
            }
        }
    })

private fun Statement.readPragma(name: String): String = executeQuery("PRAGMA $name").use { rows ->
    rows.next() shouldBe true
    rows.getString(1)
}
