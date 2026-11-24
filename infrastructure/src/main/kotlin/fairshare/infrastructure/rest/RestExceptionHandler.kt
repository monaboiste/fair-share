package fairshare.infrastructure.rest

import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupFailure
import fairshare.domain.group.exception.InvalidGroupNameException
import fairshare.domain.group.exception.InvalidSettlementCurrencyException
import fairshare.infrastructure.rest.model.FieldProblem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

@RestControllerAdvice
internal class RestExceptionHandler(@Qualifier("problemOrigin") problemOrigin: URI) : ResponseEntityExceptionHandler() {
    private val responses = ProblemResponses(problemOrigin)

    @ExceptionHandler
    fun request(failure: GroupRequestFailure): ResponseEntity<ProblemDetail> =
        responses.problem(ProblemType.INVALID_REQUEST, errors = failure.errors)

    @ExceptionHandler(MissingGroupPrecondition::class)
    fun missingPrecondition(): ResponseEntity<ProblemDetail> = responses.problem(ProblemType.PRECONDITION_REQUIRED)

    @ExceptionHandler
    fun group(failure: GroupFailure): ResponseEntity<ProblemDetail> = when (failure) {
        is GroupFailure.NotFound -> responses.problem(ProblemType.GROUP_NOT_FOUND)
        is GroupFailure.RevisionMismatch -> responses.problem(ProblemType.GROUP_VERSION_MISMATCH)
    }

    @ExceptionHandler(InvalidGroupNameException::class)
    fun invalidName(): ResponseEntity<ProblemDetail> =
        responses.problem(ProblemType.INVALID_GROUP, errors = listOf(FieldCode.BLANK_DISPLAY_NAME.at("#/displayName")))

    @ExceptionHandler(InvalidSettlementCurrencyException::class)
    fun invalidCurrency(): ResponseEntity<ProblemDetail> = responses.problem(
        ProblemType.INVALID_GROUP,
        errors = listOf(FieldCode.INVALID_SETTLEMENT_CURRENCY.at("#/settlementCurrency")),
    )

    @ExceptionHandler
    fun unknownAppend(failure: GroupAppendOutcomeUnknown): ResponseEntity<ProblemDetail> {
        diagnostics.error("Group append outcome unknown", failure)
        return responses.problem(
            ProblemType.INTERNAL_ERROR,
            "The write may have committed. Refresh and inspect current Group state before resubmitting.",
        )
    }

    @ExceptionHandler
    fun unexpected(failure: Exception): ResponseEntity<ProblemDetail> {
        diagnostics.error("Group request failed", failure)
        return responses.problem(ProblemType.INTERNAL_ERROR)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? = invalidRequest(ex, request, listOf(unreadableBody(ex.cause)))

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? = invalidRequest(ex, request, listOf(FieldCode.REQUIRED_FIELD.at("#")))

    private fun invalidRequest(ex: Exception, request: WebRequest, errors: List<FieldProblem>): ResponseEntity<Any>? {
        val type = ProblemType.INVALID_REQUEST
        val body = responses.body(type, type.detail, errors)
        return handleExceptionInternal(ex, body, problemHeaders(), type.status, request)
    }

    private companion object {
        val diagnostics: Logger = LoggerFactory.getLogger(RestExceptionHandler::class.java)
    }
}

private class ProblemResponses(private val problemOrigin: URI) {

    fun problem(
        type: ProblemType,
        detail: String = type.detail,
        errors: List<FieldProblem> = emptyList(),
    ): ResponseEntity<ProblemDetail> = ResponseEntity(body(type, detail, errors), problemHeaders(), type.status)

    fun body(type: ProblemType, detail: String, errors: List<FieldProblem>): ProblemDetail {
        val body = ProblemDetail.forStatusAndDetail(type.status, detail)
        body.title = type.title
        body.type = problemOrigin.resolve("/problems/${type.slug}")
        if (errors.isNotEmpty()) body.setProperty("errors", errors)
        return body
    }
}

private fun problemHeaders(): HttpHeaders = HttpHeaders().apply {
    contentType = MediaType.APPLICATION_PROBLEM_JSON
    setCacheControl(CacheControl.noStore())
}
