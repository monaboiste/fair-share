package fairshare.infrastructure.persistence

import java.nio.file.Files
import java.sql.DriverManager

/** Provides a temporary database for persistence wiring tests. Close it after the test to remove its files. */
class PersistenceTestDatabase : AutoCloseable {
    private val directory = Files.createTempDirectory("fair-share-persistence-test")

    /** Application properties needed to connect to this database. */
    val properties: Map<String, String>

    init {
        val jdbcUrl = "jdbc:sqlite:${directory.resolve("context.db").toUri().toASCIIString()}"
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.createStatement().use { it.execute("PRAGMA user_version = 1") }
        }
        properties = mapOf("fair-share.persistence.sqlite.jdbc-url" to jdbcUrl)
    }

    override fun close() {
        directory.toFile().deleteRecursively()
    }
}
