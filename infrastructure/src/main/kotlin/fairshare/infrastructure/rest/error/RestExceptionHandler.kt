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
import java.util.Locale

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
        val problem = problem(ProblemType.INVALID_REQUEST, request.locale)
        val cause = ex.cause

        if (cause is MismatchedInputException && cause.path.isNotEmpty()) {
            val segments = cause.path.map { it.propertyName ?: it.index.toString() }
            val code = if (cause is UnrecognizedPropertyException) UNSUPPORTED_FIELD else INVALID_TYPE
            val error = fieldError(segments, code, request.locale)

            problem.setProperty("errors", listOf(error))
        }

        return handleExceptionInternal(ex, problem, headers, status, request)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val problem = problem(ProblemType.INVALID_REQUEST, request.locale)
        val errors = ex.bindingResult.fieldErrors.map { error ->
            val segments = FIELD_PATH.findAll(error.field).map { it.value }.toList()

            if (error.code == "NotNull") {
                fieldError(segments.dropLast(1), REQUIRED_FIELD, request.locale)
            } else {
                fieldError(segments, INVALID_VALUE, request.locale)
            }
        }

        val objectErrors = ex.bindingResult.globalErrors.map {
            fieldError(emptyList(), INVALID_VALUE, request.locale)
        }

        problem.setProperty("errors", errors + objectErrors)

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
                ?: problem(ProblemType.INVALID_REQUEST, request.locale)

            HttpStatus.INTERNAL_SERVER_ERROR -> {
                log.error("Unexpected REST failure", ex)
                problem(ProblemType.INTERNAL_ERROR, request.locale)
            }

            else -> body
        }

        return super.handleExceptionInternal(ex, responseBody, headers, statusCode, request)
    }

    private fun problem(type: ProblemType, locale: Locale): ProblemDetail {
        val detail = messages.getMessage(type.messageKey, null, type.defaultDetail, locale) ?: type.defaultDetail
        val problem = ProblemDetail.forStatusAndDetail(type.status, detail)

        problem.type = problemOrigin.resolve(type.path)
        problem.title = type.title

        return problem
    }

    private fun fieldError(segments: List<String>, code: String, locale: Locale): FieldError {
        val pointer = segments.joinToString(separator = "") { "/" + it.replace("~", "~0").replace("/", "~1") }
        val fragment = URI(null, null, "", null, pointer).toASCIIString()
        val detail = messages.getMessage(code, null, "Invalid request input.", locale) ?: "Invalid request input."

        return FieldError(fragment, code, detail)
    }

    private companion object {
        const val REQUIRED_FIELD = "error.validation.required-field"
        const val INVALID_TYPE = "error.validation.invalid-type"
        const val UNSUPPORTED_FIELD = "error.validation.unsupported-field"
        const val INVALID_VALUE = "error.validation.invalid-value"

        val FIELD_PATH = Regex("[^.\\[\\]]+")
    }
}
