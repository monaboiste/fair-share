package fairshare.infrastructure.rest.example

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Locale

@SpringBootTest(
    classes = [ExampleMvcTestConfiguration::class],
    properties = ["fair-share.rest.problem-origin=https://docs.example.org"],
)
@AutoConfigureMockMvc
@ApplyExtension(SpringExtension::class)
class ExampleProblemDetailsTest : ExpectSpec() {
    @Autowired
    private lateinit var mvc: MockMvc

    init {
        expect("the existing Example API returns its normal response") {
            mvc.perform(get("/example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("test"))
        }

        expect("a valid typed request succeeds") {
            mvc.perform(post("/example/echo").contentType(MediaType.APPLICATION_JSON).content("""{"message":"test"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("test"))
        }

        expect("a missing required field points to its containing object") {
            val response = invalidRequest("{}")

            response
                .andExpect(jsonPath("$.errors[0].pointer").value("#"))
                .andExpect(jsonPath("$.errors[0].code").value("error.validation.required-field"))
                .andExpect(jsonPath("$.errors[0].detail").value("A required field is missing."))
        }

        expect("a forbidden null points to the field") {
            val response = invalidRequest("""{"message":null}""")

            response
                .andExpect(jsonPath("$.errors[0].pointer").value("#/message"))
                .andExpect(jsonPath("$.errors[0].code").value("error.validation.invalid-type"))
        }

        expect("wrong JSON types are rejected instead of coerced") {
            listOf("true", "42", "1.5", "{}").forEach { value ->
                val response = invalidRequest("""{"message":$value}""")

                response
                    .andExpect(jsonPath("$.errors[0].pointer").value("#/message"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.validation.invalid-type"))
                    .andExpect(jsonPath("$.errors[0].detail").value("The field has an invalid type."))
            }
        }

        expect("an invalid array element points to its index") {
            invalidRequest("""{"message":"test","tags":["valid",false]}""")
                .andExpect(jsonPath("$.errors[0].pointer").value("#/tags/1"))
                .andExpect(jsonPath("$.errors[0].code").value("error.validation.invalid-type"))
        }

        expect("unknown field names use escaped JSON Pointer fragments") {
            val response = invalidRequest("""{"message":"test","a/b~c #":true}""")

            response
                .andExpect(jsonPath("$.errors[0].pointer").value("#/a~1b~0c%20%23"))
                .andExpect(jsonPath("$.errors[0].code").value("error.validation.unsupported-field"))
                .andExpect(jsonPath("$.errors[0].detail").value("The field is not supported."))
        }

        expect("Bean Validation reports invalid field values") {
            val response = invalidRequest("""{"message":""}""")

            response
                .andExpect(jsonPath("$.errors[0].pointer").value("#/message"))
                .andExpect(jsonPath("$.errors[0].code").value("error.validation.invalid-value"))
                .andExpect(jsonPath("$.errors[0].detail").value("The field has an invalid value."))
        }

        expect("malformed JSON, duplicate fields and trailing tokens return 400") {
            listOf(
                "{",
                """{"message":"first","message":"second"}""",
                """{"message":"test"} {}""",
                "[]",
            ).forEach { body ->
                invalidRequest(body).andExpect(jsonPath("$.errors").doesNotExist())
            }
        }

        expect("a missing query parameter returns the invalid request problem") {
            mvc.perform(get("/example/echo"))
                .expectInvalidRequestProblem()
                .andExpect(jsonPath("$.errors").doesNotExist())
        }

        expect("a framework bad request replaces foreign problem details") {
            mvc.perform(get("/example/framework-bad-request"))
                .expectInvalidRequestProblem("/example/framework-bad-request")
                .andExpect(jsonPath("$.errors").doesNotExist())
        }

        expect("unsupported methods preserve the framework status and Allow header") {
            mvc.perform(post("/example"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
        }

        expect("a missing endpoint preserves the framework status") {
            mvc.perform(get("/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
        }

        expect("unexpected and framework internal failures share one safe response") {
            listOf("/example/failure", "/example/framework-failure").forEach { path ->
                mvc.perform(get(path))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("https://docs.example.org/problems/internal-error"))
                    .andExpect(jsonPath("$.title").value("Internal server error"))
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                    .andExpect(jsonPath("$.instance").value(path))
                    .andExpect(jsonPath("$.code").doesNotExist())
                    .andExpect(jsonPath("$.errors").doesNotExist())
            }
        }

        expect("public details use the request locale") {
            mvc.perform(get("/example/failure").locale(Locale.CANADA_FRENCH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Une erreur inattendue est survenue."))
        }

        expect("problem type documentation is available") {
            mvc.perform(get("/problems/invalid-request"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(
                    content().string(
                        "Invalid request (400). Correct the request before resubmitting. " +
                            "Field errors identify invalid input.",
                    ),
                )

            mvc.perform(get("/problems/internal-error"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(
                    content().string(
                        "Internal server error (500). Refresh and inspect the current state " +
                            "before considering resubmission of a write.",
                    ),
                )
        }
    }

    private fun invalidRequest(body: String): ResultActions = mvc.perform(
        post("/example/echo")
            .header("Host", "untrusted.example.org")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
    ).expectInvalidRequestProblem()

    private fun ResultActions.expectInvalidRequestProblem(path: String = "/example/echo"): ResultActions =
        andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://docs.example.org/problems/invalid-request"))
            .andExpect(jsonPath("$.title").value("Invalid request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("The request is invalid."))
            .andExpect(jsonPath("$.instance").value(path))
            .andExpect(jsonPath("$.code").doesNotExist())
}
