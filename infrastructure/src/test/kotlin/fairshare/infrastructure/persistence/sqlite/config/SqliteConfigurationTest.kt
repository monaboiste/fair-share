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
            expect("connections preserve WAL and apply foreign keys, busy timeout and synchronous $synchronous") {
                val databaseFile = tempdir().resolve("configured.db").apply {
                    prepareDatabase(version = 1, journalMode = SQLiteConfig.JournalMode.WAL)
                }
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

        expect("application connections preserve DELETE journaling") {
            val databaseFile = tempdir().resolve("configured.db").apply { prepareDatabase(version = 1) }
            val properties = SqliteProperties(
                jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}",
                busyTimeoutMillis = 1234,
                synchronous = SQLiteConfig.SynchronousMode.FULL,
            )
            val dataSource = SqliteConfiguration().dataSource(properties)
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

        listOf(0, 2).forEach { version ->
            expect("opening the application data source rejects schema version $version") {
                val databaseFile = tempdir().resolve("incompatible.db").apply { prepareDatabase(version) }

                shouldThrow<IllegalStateException> { openDataSource(databaseFile) }.message shouldBe
                    "Incompatible SQLite schema version: expected 1, found $version"
            }
        }
    })

private fun openDataSource(file: File) = SqliteConfiguration().dataSource(
    SqliteProperties("jdbc:sqlite:${file.absolutePath}", 5000, SQLiteConfig.SynchronousMode.FULL),
)

private fun File.prepareDatabase(
    version: Int,
    journalMode: SQLiteConfig.JournalMode = SQLiteConfig.JournalMode.DELETE,
) {
    DriverManager.getConnection("jdbc:sqlite:$absolutePath").use { connection ->
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA user_version = $version")
            statement.execute("PRAGMA journal_mode = $journalMode")
        }
    }
}

private fun Statement.readPragma(name: String): String = executeQuery("PRAGMA $name").use { rows ->
    rows.next() shouldBe true
    rows.getString(1)
}
