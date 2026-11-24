package fairshare.infrastructure.rest.example

import fairshare.infrastructure.rest.example.model.ExampleResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
internal class ExampleProblemFixtureController {
    @PostMapping("/example/echo")
    fun echo(@Valid @RequestBody request: ExampleRequest): ExampleResponse =
        ExampleResponse(checkNotNull(request.message))

    @GetMapping("/example/echo")
    fun echoQuery(@RequestParam message: String): ExampleResponse = ExampleResponse(message)

    @GetMapping("/example/failure")
    fun failure(): ExampleResponse = error("Private database details")

    @GetMapping("/example/framework-bad-request")
    fun frameworkBadRequest(): ExampleResponse {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Framework validation details")

        throw ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null)
    }

    @GetMapping("/example/framework-failure")
    fun frameworkFailure(): ExampleResponse {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Private storage details")

        throw ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR, problem, null)
    }
}
