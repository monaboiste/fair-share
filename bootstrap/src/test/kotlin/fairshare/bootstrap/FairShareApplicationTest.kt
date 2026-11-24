package fairshare.bootstrap

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.io.File

class FairShareApplicationTest :
    ExpectSpec({
        expect("the application context starts with an isolated database") {
            startBackend(
                tempdir().resolve("smoke.db"),
                "server.port" to "0",
                webApplicationType = WebApplicationType.SERVLET,
            ).use { context ->
                context.isActive shouldBe true
            }
        }
    })

internal fun startBackend(
    database: File,
    vararg overrides: Pair<String, String>,
    webApplicationType: WebApplicationType = WebApplicationType.NONE,
): ConfigurableApplicationContext {
    val settings = mapOf("fair-share.persistence.sqlite.jdbc-url" to "jdbc:sqlite:$database") + overrides
    return SpringApplicationBuilder(FairShareApplication::class.java)
        .web(webApplicationType)
        .properties("spring.main.banner-mode=off", "logging.level.root=OFF")
        .run(*settings.map { (key, value) -> "--$key=$value" }.toTypedArray())
}
