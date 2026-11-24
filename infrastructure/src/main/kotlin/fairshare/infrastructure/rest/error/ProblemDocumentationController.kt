package fairshare.infrastructure.rest.error

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class ProblemDocumentationController : ProblemDocumentationApi {
    @GetMapping("/problems/invalid-request", produces = [MediaType.TEXT_PLAIN_VALUE])
    override fun invalidRequest(): String =
        "Invalid request (400). Correct the request before resubmitting. Field errors identify invalid input."

    @GetMapping("/problems/internal-error", produces = [MediaType.TEXT_PLAIN_VALUE])
    override fun internalError(): String =
        "Internal server error (500). Refresh and inspect the current state before considering resubmission of a write."
}
