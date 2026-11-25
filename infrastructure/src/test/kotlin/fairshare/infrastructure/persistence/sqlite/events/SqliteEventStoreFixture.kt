package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent
import fairshare.domain.events.EventEnvelope
import fairshare.domain.events.EventId
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import fairshare.infrastructure.persistence.sqlite.config.SqliteConfiguration
import fairshare.infrastructure.persistence.sqlite.config.SqliteProperties
import org.springframework.transaction.support.TransactionTemplate
import org.sqlite.SQLiteConfig
import java.io.File
import java.time.Instant
import javax.sql.DataSource

internal class SqliteEventStoreFixture(
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
    val dataSource = configuration.dataSource(properties)
    val transactions = configuration.transactionTemplate(configuration.transactionManager(dataSource))
    val store = store(transactions = transactions)

    init {
        SqliteEventSchema.initialize(dataSource)
    }

    fun reopen(): SqliteEventStore = store(configuration.dataSource(properties))

    fun store(
        source: DataSource = dataSource,
        codec: EventCodec = TestEventCodec(),
        transactions: TransactionTemplate = configuration.transactionTemplate(configuration.transactionManager(source)),
    ): SqliteEventStore = SqliteEventStore(configuration.namedParameterJdbcTemplate(source), transactions, codec)

    fun executeSql(vararg commands: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                commands.forEach { statement.execute(it) }
            }
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
