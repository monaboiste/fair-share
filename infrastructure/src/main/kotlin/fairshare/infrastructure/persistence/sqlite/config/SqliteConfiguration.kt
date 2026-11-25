package fairshare.infrastructure.persistence.sqlite.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SqliteProperties::class)
internal class SqliteConfiguration {

    @Bean
    fun dataSource(properties: SqliteProperties): DataSource {
        val config = SQLiteConfig()
        config.setJournalMode(properties.journalMode)
        config.enforceForeignKeys(true)
        config.setBusyTimeout(properties.busyTimeoutMillis)
        config.setSynchronous(properties.synchronous)

        return SQLiteDataSource(config).apply {
            url = properties.jdbcUrl
        }
    }

    @Bean
    fun namedParameterJdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate =
        NamedParameterJdbcTemplate(dataSource)

    @Bean
    fun transactionManager(dataSource: DataSource): DataSourceTransactionManager = SqliteTransactionManager(dataSource)

    @Bean
    fun transactionTemplate(transactionManager: DataSourceTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager)
}
