package fairshare.infrastructure.persistence

import fairshare.infrastructure.persistence.sqlite.SqliteEventSchema
import org.sqlite.SQLiteDataSource
import java.nio.file.Files

/** Provides a temporary database for persistence wiring tests. Close it after the test to remove its files. */
class PersistenceTestDatabase : AutoCloseable {
    private val directory = Files.createTempDirectory("fair-share-persistence-test")

    /** Application properties needed to connect to this database. */
    val properties: Map<String, String>

    init {
        val jdbcUrl = "jdbc:sqlite:${directory.resolve("context.db").toUri().toASCIIString()}"
        SqliteEventSchema.initialize(SQLiteDataSource().apply { url = jdbcUrl })
        properties = mapOf("fair-share.persistence.sqlite.jdbc-url" to jdbcUrl)
    }

    override fun close() {
        directory.toFile().deleteRecursively()
    }
}
