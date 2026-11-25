package fairshare.infrastructure.persistence.sqlite.events

import fairshare.infrastructure.persistence.sqlite.config.SqliteConfiguration
import org.springframework.transaction.support.DefaultTransactionDefinition
import javax.sql.DataSource

internal fun testEventStore(
    source: DataSource,
    codec: EventCodec = TestEventCodec(),
    configureTransactions: DefaultTransactionDefinition.() -> Unit = {},
): SqliteEventStore {
    val configuration = SqliteConfiguration()
    val transactions = configuration.transactionTemplate(configuration.transactionManager(source))
        .apply(configureTransactions)
    return SqliteEventStore(configuration.namedParameterJdbcTemplate(source), transactions, codec)
}
