package fairshare.bootstrap.persistence

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import org.sqlite.SQLiteConfig

class PersistenceConfigurationTest :
    ExpectSpec({
        expect("the configured data source opens a SQLite connection") {
            val properties = PersistenceProperties(
                jdbcUrl = "jdbc:sqlite::memory:",
                busyTimeoutMillis = 5000,
                synchronous = SQLiteConfig.SynchronousMode.FULL,
            )
            val dataSource = PersistenceConfiguration().dataSource(properties)

            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 1").use { rows ->
                        rows.next() shouldBe true
                        rows.getInt(1) shouldBe 1
                    }
                }
            }
        }
    })
