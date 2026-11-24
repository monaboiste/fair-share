package fairshare.bootstrap

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "fair-share.persistence.sqlite.jdbc-url=jdbc:sqlite::memory:",
        "management.defaults.metrics.export.enabled=false",
    ],
)
@ApplyExtension(SpringExtension::class)
class ApplicationContextTest :
    BehaviorSpec({
        given("the application") {
            then("spring context starts successfully") {
            }
        }
    })
