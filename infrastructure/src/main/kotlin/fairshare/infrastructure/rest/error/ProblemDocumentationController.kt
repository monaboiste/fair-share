package fairshare.infrastructure.rest.error

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class ProblemDocumentationController : ProblemDocumentationApi {

    @GetMapping("/problems/invalid-request", produces = [MediaType.TEXT_MARKDOWN_VALUE])
    // language=markdown
    override fun invalidRequest(): String = """
        # Invalid Request (400)

        The request could not be processed due to invalid input data or malformed syntax.

        ## Resolution
        * Correct the request payload or parameters before resubmitting.
        * Check the `errors` or field-level details in the response to identify invalid input.

        ## Example Response
        ```json
        {
          "type": "/problems/invalid-request",
          "title": "Invalid request",
          "status": 400,
          "detail": "Field errors identify invalid input."
        }
        ```
    """.trimIndent()

    @GetMapping("/problems/internal-error", produces = [MediaType.TEXT_MARKDOWN_VALUE])
    // language=markdown
    override fun internalError(): String = """
        # Internal Server Error (500)

        An unexpected error occurred on the server while processing the request.

        ## Resolution
        * Refresh and inspect the current state of the resource before considering resubmission of a write operation
          (`POST`, `PUT`, `PATCH`, `DELETE`).
        * Safe/idempotent operations (`GET`) can be retried after a short delay.

        ## Example Response
        ```json
        {
          "type": "/problems/internal-error",
          "title": "Internal server error",
          "status": 500,
          "detail": "An unexpected error occurred."
        }
        ```
    """.trimIndent()
}
