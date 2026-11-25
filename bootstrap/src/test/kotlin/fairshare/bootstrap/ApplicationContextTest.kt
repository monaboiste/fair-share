package fairshare.bootstrap

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import javax.sql.DataSource

@SpringBootTest(
    properties = [
        "fair-share.persistence.sqlite.jdbc-url=jdbc:sqlite::memory:",
        "management.defaults.metrics.export.enabled=false",
    ],
)
@ApplyExtension(SpringExtension::class)
class ApplicationContextTest : ExpectSpec() {

    @Autowired
    private lateinit var context: ApplicationContext

    init {
        expect("spring context starts successfully") {}
        expect("datasource is configured") {
            context.getBeansOfType(DataSource::class.java)
                .size shouldBe 1
        }
    }
}
