package fairshare.infrastructure.persistence.sqlite.events

import fairshare.infrastructure.persistence.sqlite.SqliteEventSchema
import fairshare.infrastructure.persistence.sqlite.config.SqliteConfiguration
import fairshare.infrastructure.persistence.sqlite.config.SqliteProperties
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File
import javax.sql.DataSource

internal class TestSqliteDatabase(
    databaseFile: File,
    busyTimeoutMillis: Int = 5_000,
    journalMode: SQLiteConfig.JournalMode = SQLiteConfig.JournalMode.WAL,
) {
    private val configuration = SqliteConfiguration()
    private val properties = SqliteProperties(
        jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}",
        busyTimeoutMillis = busyTimeoutMillis,
        synchronous = SQLiteConfig.SynchronousMode.FULL,
        journalMode = journalMode,
    )
    val dataSource: DataSource

    init {
        SqliteEventSchema.initialize(SQLiteDataSource().apply { url = properties.jdbcUrl })
        dataSource = newDataSource()
    }

    fun newDataSource(): DataSource = configuration.dataSource(properties)

    fun initializeSchema() = SqliteEventSchema.initialize(dataSource)

    fun executeSql(vararg commands: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                commands.forEach { statement.execute(it) }
            }
        }
    }
}
