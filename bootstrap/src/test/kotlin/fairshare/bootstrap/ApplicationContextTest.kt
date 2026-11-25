package fairshare.bootstrap

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.nio.file.Files
import javax.sql.DataSource

@SpringBootTest(properties = ["management.defaults.metrics.export.enabled=false"])
@ApplyExtension(SpringExtension::class)
class ApplicationContextTest : ExpectSpec() {

    @Autowired
    private lateinit var context: ApplicationContext

    init {
        expect("SQLite JDBC and transaction beans are singletons wired to the same data source") {
            val dataSource = context.getBeansOfType(DataSource::class.java).values.single()
            val jdbc = context.getBeansOfType(NamedParameterJdbcTemplate::class.java).values.single()
            val transactionManager = context.getBeansOfType(DataSourceTransactionManager::class.java).values.single()
            val transactionTemplate = context.getBeansOfType(TransactionTemplate::class.java).values.single()

            context.getBean("namedParameterJdbcTemplate") shouldBeSameInstanceAs jdbc
            context.getBean("transactionManager") shouldBeSameInstanceAs transactionManager
            context.getBean("transactionTemplate") shouldBeSameInstanceAs transactionTemplate

            jdbc.jdbcTemplate.dataSource shouldBeSameInstanceAs dataSource
            transactionManager.dataSource shouldBeSameInstanceAs dataSource
            transactionTemplate.transactionManager shouldBeSameInstanceAs transactionManager
        }

        expect("file-backed SQLite uses WAL journaling by default") {
            val jdbc = context.getBean(NamedParameterJdbcTemplate::class.java)

            jdbc.queryForObject("PRAGMA journal_mode", emptyMap<String, Any>(), String::class.java) shouldBe "wal"
            databaseFile.isFile shouldBe true
        }

        afterSpec { databaseFile.parentFile.deleteRecursively() }
    }

    companion object {
        private val databaseFile = Files.createTempDirectory("fair-share-context-test").resolve("context.db").toFile()

        @JvmStatic
        @DynamicPropertySource
        fun sqliteProperties(registry: DynamicPropertyRegistry) {
            registry.add("fair-share.persistence.sqlite.jdbc-url") { "jdbc:sqlite:${databaseFile.absolutePath}" }
        }
    }
}
