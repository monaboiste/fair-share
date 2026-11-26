package fairshare.infrastructure.persistence.sqlite.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import org.sqlite.SQLiteOpenMode
import javax.sql.DataSource

private const val EXPECTED_SCHEMA_VERSION = 1

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SqliteProperties::class)
internal class SqliteConfiguration {

    @Bean
    fun dataSource(properties: SqliteProperties): DataSource {
        val config = SQLiteConfig()
        config.resetOpenMode(SQLiteOpenMode.CREATE)
        config.setJournalMode(properties.journalMode)
        config.enforceForeignKeys(true)
        config.setBusyTimeout(properties.busyTimeoutMillis)
        config.setSynchronous(properties.synchronous)

        val dataSource = SQLiteDataSource(config).apply {
            url = properties.jdbcUrl
        }
        val version = JdbcTemplate(dataSource).queryForObject("PRAGMA user_version", Int::class.javaObjectType)
        check(EXPECTED_SCHEMA_VERSION == version) {
            "Incompatible SQLite schema version: expected $EXPECTED_SCHEMA_VERSION, found $version"
        }
        return dataSource
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
