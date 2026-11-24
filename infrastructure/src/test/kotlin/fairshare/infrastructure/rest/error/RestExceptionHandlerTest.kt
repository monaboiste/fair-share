package fairshare.infrastructure.rest.error

import fairshare.infrastructure.rest.example.ExampleProblemFixtureController
import fairshare.infrastructure.rest.example.ExampleRequest
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.MethodParameter
import org.springframework.http.ProblemDetail
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import java.net.URI

class RestExceptionHandlerTest :
    ExpectSpec({
        expect("missing translations still produce a safe internal error detail") {
            val handler = RestExceptionHandler(StaticMessageSource(), URI.create("https://docs.example.org"))
            val request = ServletWebRequest(MockHttpServletRequest())

            val response = handler.handleUnexpected(IllegalStateException("Private storage details"), request)

            response?.body.shouldBeTypeOf<ProblemDetail>().detail shouldBe "An unexpected error occurred."
        }

        expect("object validation failures point to the request root and use safe fallback details") {
            val handler = RestExceptionHandler(StaticMessageSource(), URI.create("https://docs.example.org"))
            val request = ServletWebRequest(MockHttpServletRequest())
            val binding = BeanPropertyBindingResult(ExampleRequest(message = "test"), "exampleRequest")
            binding.reject("private-validation-code", "Private validation details")
            val method = ExampleProblemFixtureController::class.java.getDeclaredMethod(
                "echo",
                ExampleRequest::class.java,
            )

            val response = handler.handleException(
                MethodArgumentNotValidException(MethodParameter(method, 0), binding),
                request,
            )

            val problem = response?.body.shouldBeTypeOf<ProblemDetail>()
            problem.detail shouldBe "The request is invalid."
            problem.properties?.get("errors") shouldBe
                listOf(FieldError("#", "error.validation.invalid-value", "Invalid request input."))
        }
    })
