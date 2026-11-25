package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent
import fairshare.domain.events.EventEnvelope
import fairshare.domain.events.EventId
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File
import java.time.Instant

internal class SqliteEventStoreFixture(
    private val databaseFile: File,
    private val busyTimeoutMillis: Int = 5_000,
    private val journalMode: SQLiteConfig.JournalMode = SQLiteConfig.JournalMode.WAL,
) {
    val dataSource = openDataSource()
    val store = SqliteEventStore(dataSource, TestEventCodec())

    init {
        SqliteEventSchema.initialize(dataSource)
    }

    fun reopen(): SqliteEventStore = SqliteEventStore(openDataSource(), TestEventCodec())

    fun executeSql(vararg commands: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                commands.forEach { statement.execute(it) }
            }
        }
    }

    private fun openDataSource(): SQLiteDataSource {
        val config = SQLiteConfig().apply {
            setJournalMode(journalMode)
            setSynchronous(SQLiteConfig.SynchronousMode.FULL)
            setBusyTimeout(busyTimeoutMillis)
            enforceForeignKeys(true)
        }
        return SQLiteDataSource(config).apply {
            url = "jdbc:sqlite:${databaseFile.absolutePath}"
        }
    }
}

internal fun pending(
    payload: DomainEvent,
    recordedAt: Instant = Instant.parse("2026-03-05T10:15:30.123456789Z"),
): PendingEvent = PendingEvent(EventId.random(), recordedAt, payload)

internal fun PendingEvent.envelope(streamId: StreamId, sequence: Long, revision: Long): EventEnvelope = EventEnvelope(
    eventId = eventId,
    streamId = streamId,
    sequence = sequence,
    revision = StreamRevision(revision),
    recordedAt = recordedAt,
    payload = payload,
)
