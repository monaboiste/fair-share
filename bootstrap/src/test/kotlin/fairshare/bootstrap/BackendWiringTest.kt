package fairshare.bootstrap

import fairshare.application.group.CreateGroupUseCase
import fairshare.application.group.GetGroupUseCase
import fairshare.infrastructure.rest.RestSettings
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

class BackendWiringTest :
    ExpectSpec({
        expect("typed groups bind into the application, SQLite store and single strict Jackson mapper") {
            val db = tempdir().resolve("configured.db")
            val jdbcUrl = "jdbc:sqlite:$db"
            val origin = "https://problems.example.test"
            SpringApplicationBuilder(FairShareApplication::class.java)
                .web(WebApplicationType.NONE)
                .properties(
                    mapOf(
                        "spring.main.banner-mode" to "off",
                        "logging.level.root" to "WARN",
                        "fair-share.database-url" to "jdbc:sqlite:${tempdir().resolve("unused.db")}",
                        "fair-share.persistence.sqlite.busy-timeout-millis" to "125",
                        "fair-share.persistence.sqlite.synchronous" to "full",
                        "fair-share.rest.problem-origin" to origin,
                    ),
                ).run("--fair-share.persistence.sqlite.jdbc-url=$jdbcUrl").use { context ->
                    val rest = context.getBean(RestSettings::class.java)
                    rest.problemOrigin shouldBe origin
                    val configured = context.getBean(PersistenceProperties::class.java).sqlite
                    configured.jdbcUrl shouldBe jdbcUrl
                    configured.busyTimeoutMillis shouldBe 125
                    configured.synchronous.name shouldBe "FULL"
                    val mapper = context.getBeansOfType(JsonMapper::class.java).values.single()
                    mapper.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS) shouldBe true
                    mapper.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY) shouldBe true
                    val created = context.getBean(CreateGroupUseCase::class.java).execute("Trip", "EUR")
                    context.getBean(GetGroupUseCase::class.java).execute(created.id).revision shouldBe created.revision
                    db.exists() shouldBe true
                }
        }
    })
