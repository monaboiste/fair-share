package fairshare.bootstrap.persistence

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.sqlite.SQLiteConfig
import java.sql.Statement

class PersistenceConfigurationTest :
    ExpectSpec({
        val databaseFile = tempdir().resolve("configured.db")

        expect("SQLite opens the configured database with the configured connection settings") {
            val properties = PersistenceProperties(
                jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}",
                busyTimeoutMillis = 1234,
                synchronous = SQLiteConfig.SynchronousMode.OFF,
            )
            val dataSource = PersistenceConfiguration().dataSource(properties)

            dataSource.connection.use { connection ->
                databaseFile.isFile shouldBe true
                connection.createStatement().use { statement ->
                    statement.readPragma("busy_timeout") shouldBe 1234
                    statement.readPragma("synchronous") shouldBe 0
                }
            }
        }
    })

private fun Statement.readPragma(name: String): Int = executeQuery("PRAGMA $name").use { rows ->
    rows.next() shouldBe true
    rows.getInt(1)
}
