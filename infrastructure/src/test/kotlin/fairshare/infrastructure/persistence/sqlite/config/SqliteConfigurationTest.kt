package fairshare.infrastructure.persistence.sqlite.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.sqlite.SQLiteConfig
import java.io.File
import java.sql.DriverManager
import java.sql.Statement

class SqliteConfigurationTest :
    ExpectSpec({
        val synchronousModes = mapOf(SQLiteConfig.SynchronousMode.FULL to "2", SQLiteConfig.SynchronousMode.OFF to "0")

        synchronousModes.forEach { (synchronous, pragmaValue) ->
            expect("each SQLite connection uses WAL with foreign keys, a busy timeout and synchronous $synchronous") {
                val databaseFile = tempdir().resolve("configured.db").apply { setSchemaVersion(1) }
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
            val databaseFile = tempdir().resolve("configured.db").apply { setSchemaVersion(1) }
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

        expect("opening the application data source does not create a missing database") {
            val missing = tempdir().resolve("missing.db")

            shouldThrow<CannotGetJdbcConnectionException> { openDataSource(missing) }

            missing.exists() shouldBe false
        }

        expect("opening the application data source rejects an incompatible schema version") {
            val databaseFile = tempdir().resolve("incompatible.db").apply { setSchemaVersion(2) }

            shouldThrow<IllegalStateException> { openDataSource(databaseFile) }.message shouldBe
                "Incompatible SQLite schema version: expected 1, found 2"
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

private fun openDataSource(file: File) = SqliteConfiguration().dataSource(
    SqliteProperties("jdbc:sqlite:${file.absolutePath}", 5000, SQLiteConfig.SynchronousMode.FULL),
)

private fun File.setSchemaVersion(version: Int) {
    DriverManager.getConnection("jdbc:sqlite:$absolutePath").use { connection ->
        connection.createStatement().use { it.execute("PRAGMA user_version = $version") }
    }
}

private fun Statement.readPragma(name: String): String = executeQuery("PRAGMA $name").use { rows ->
    rows.next() shouldBe true
    rows.getString(1)
}
