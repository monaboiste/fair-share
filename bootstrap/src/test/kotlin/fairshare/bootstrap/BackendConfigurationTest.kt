package fairshare.bootstrap

import fairshare.application.group.CreateGroupUseCase
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.springframework.boot.context.properties.bind.validation.BindValidationException
import javax.sql.DataSource

class BackendConfigurationTest :
    ExpectSpec({
        expect("SQLite can write to the configured file with the requested wait time and durability") {
            val database = tempdir().resolve("configured.db")
            startBackend(
                database,
                "fair-share.persistence.sqlite.busy-timeout-millis" to "125",
                "fair-share.persistence.sqlite.synchronous" to "extra",
                "fair-share.rest.problem-origin" to "HTTPS://problems.example.test:65535/",
            ).use { context ->
                val dataSource = context.getBean(DataSource::class.java)
                readPragma(dataSource, "busy_timeout") shouldBe 125
                readPragma(dataSource, "synchronous") shouldBe 3
                context.getBean(CreateGroupUseCase::class.java).execute("Trip", "PLN")
                database.length() shouldBeGreaterThan 0L
            }
        }

        withData(
            nameFn = { "startup rejects ${it.first}=${it.second}" },
            "fair-share.persistence.sqlite.jdbc-url" to "jdbc:other:invalid",
            "fair-share.persistence.sqlite.jdbc-url" to "jdbc:sqlite::memory:",
            "fair-share.persistence.sqlite.jdbc-url" to "jdbc:sqlite:file:memory-test?mode=memory&cache=shared",
            "fair-share.persistence.sqlite.busy-timeout-millis" to "-1",
            "fair-share.rest.problem-origin" to "file://docs.example.test",
            "fair-share.rest.problem-origin" to "/",
            "fair-share.rest.problem-origin" to "https:/",
            "fair-share.rest.problem-origin" to "https://docs.example.test:65536",
            "fair-share.rest.problem-origin" to "https://user@docs.example.test",
            "fair-share.rest.problem-origin" to "https://docs.example.test/path",
            "fair-share.rest.problem-origin" to "https://docs.example.test?query",
            "fair-share.rest.problem-origin" to "https://docs.example.test#fragment",
        ) { setting ->
            val failure = shouldThrowAny {
                startBackend(tempdir().resolve("invalid.db"), setting).close()
            }
            generateSequence(failure) { it.cause }.any { it is BindValidationException } shouldBe true
        }
    })

private fun readPragma(dataSource: DataSource, name: String): Int = dataSource.connection.use { connection ->
    connection.createStatement().use { command ->
        command.executeQuery("PRAGMA $name").use { rows ->
            check(rows.next())
            rows.getInt(1)
        }
    }
}
