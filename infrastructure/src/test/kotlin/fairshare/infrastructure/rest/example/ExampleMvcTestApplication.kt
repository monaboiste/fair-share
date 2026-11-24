package fairshare.infrastructure.rest.example

import fairshare.infrastructure.rest.RestJsonConfiguration
import fairshare.infrastructure.rest.error.ProblemDocumentationController
import fairshare.infrastructure.rest.error.RestExceptionHandler
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(
    ExampleController::class,
    ExampleProblemFixtureController::class,
    RestJsonConfiguration::class,
    RestExceptionHandler::class,
    ProblemDocumentationController::class,
)
internal class ExampleMvcTestApplication
