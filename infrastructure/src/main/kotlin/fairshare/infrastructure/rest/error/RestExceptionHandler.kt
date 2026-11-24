package fairshare.infrastructure.rest.error

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.exc.UnrecognizedPropertyException
import java.net.URI

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
internal class RestExceptionHandler(
    private val messages: MessageSource,
    @Value("\${fair-share.rest.problem-origin}")
    private val problemOrigin: URI,
) : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(RestExceptionHandler::class.java)

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val problem = problem(ProblemType.INVALID_REQUEST, request)
        val cause = ex.cause

        if (cause is MismatchedInputException) {
            val segments = buildList {
                for (reference in cause.path) {
                    add(reference.propertyName ?: reference.index.toString())
                }
            }

            if (segments.isNotEmpty()) {
                val code = if (cause is UnrecognizedPropertyException) UNSUPPORTED_FIELD else INVALID_TYPE
                problem.setProperty("errors", listOf(fieldError(segments, code, request)))
            }
        }

        return handleExceptionInternal(ex, problem, headers, status, request)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val problem = problem(ProblemType.INVALID_REQUEST, request)
        val errors = buildList {
            for (error in ex.bindingResult.fieldErrors) {
                val segments = FIELD_PATH.findAll(error.field).map { it.value }.toList()

                if (error.code == "NotNull") {
                    add(fieldError(segments.dropLast(1), REQUIRED_FIELD, request))
                } else {
                    add(fieldError(segments, INVALID_VALUE, request))
                }
            }

            repeat(ex.bindingResult.globalErrorCount) {
                add(fieldError(emptyList(), INVALID_VALUE, request))
            }
        }

        problem.setProperty("errors", errors)

        return handleExceptionInternal(ex, problem, headers, status, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: WebRequest): ResponseEntity<Any>? =
        handleExceptionInternal(ex, null, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request)

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val responseBody = when (statusCode) {
            HttpStatus.BAD_REQUEST -> (body as? ProblemDetail)
                ?.takeIf { it.type == problemOrigin.resolve(ProblemType.INVALID_REQUEST.path) }
                ?: problem(ProblemType.INVALID_REQUEST, request)

            HttpStatus.INTERNAL_SERVER_ERROR -> {
                log.error("Unexpected REST failure", ex)
                problem(ProblemType.INTERNAL_ERROR, request)
            }

            else -> body
        }

        return super.handleExceptionInternal(ex, responseBody, headers, statusCode, request)
    }

    private fun problem(type: ProblemType, request: WebRequest): ProblemDetail {
        val detail = message(type.messageKey, type.defaultDetail, request)
        val problem = ProblemDetail.forStatusAndDetail(type.status, detail)

        problem.type = problemOrigin.resolve(type.path)
        problem.title = type.title

        return problem
    }

    private fun fieldError(segments: List<String>, code: String, request: WebRequest): FieldError {
        val pointer = segments.joinToString(separator = "") { "/" + it.replace("~", "~0").replace("/", "~1") }
        val fragment = URI(null, null, "", null, pointer).toASCIIString()
        val detail = message(code, "Invalid request input.", request)

        return FieldError(fragment, code, detail)
    }

    private fun message(code: String, fallback: String, request: WebRequest): String =
        messages.getMessage(code, null, null, request.locale) ?: fallback

    private companion object {
        const val REQUIRED_FIELD = "error.validation.required-field"
        const val INVALID_TYPE = "error.validation.invalid-type"
        const val UNSUPPORTED_FIELD = "error.validation.unsupported-field"
        const val INVALID_VALUE = "error.validation.invalid-value"

        val FIELD_PATH = Regex("[^.\\[\\]]+")
    }
}
