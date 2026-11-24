package fairshare.infrastructure.rest

import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupFailure
import fairshare.application.group.GroupInputViolation
import fairshare.infrastructure.rest.model.FieldProblem
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

@RestControllerAdvice
internal class GroupProblemAdvice(settings: RestSettings) : ResponseEntityExceptionHandler() {
    private val origin = settings.problemOrigin.trimEnd('/')

    @ExceptionHandler
    fun request(failure: GroupRequestFailure): ResponseEntity<ProblemDetail> =
        problem(ProblemType.INVALID_REQUEST, errors = failure.errors)

    @ExceptionHandler(MissingGroupPrecondition::class)
    fun missingPrecondition(): ResponseEntity<ProblemDetail> = problem(ProblemType.PRECONDITION_REQUIRED)

    @ExceptionHandler
    fun group(failure: GroupFailure): ResponseEntity<ProblemDetail> = when (failure) {
        is GroupFailure.NotFound -> problem(ProblemType.GROUP_NOT_FOUND)

        is GroupFailure.RevisionMismatch -> problem(ProblemType.GROUP_VERSION_MISMATCH)

        is GroupFailure.InvalidGroup -> problem(
            ProblemType.INVALID_GROUP,
            errors = failure.violations.map { it.field() },
        )
    }

    @ExceptionHandler
    fun unknownAppend(failure: GroupAppendOutcomeUnknown): ResponseEntity<ProblemDetail> {
        diagnostics.log(System.Logger.Level.ERROR, "Group append outcome unknown", failure)
        return problem(
            ProblemType.INTERNAL_ERROR,
            "The write may have committed. Refresh and inspect current Group state before resubmitting.",
        )
    }

    @ExceptionHandler
    fun unexpected(failure: RuntimeException): ResponseEntity<ProblemDetail> {
        diagnostics.log(System.Logger.Level.ERROR, "Group request failed", failure)
        return problem(ProblemType.INTERNAL_ERROR)
    }

    private fun problem(
        type: ProblemType,
        detail: String = type.detail,
        errors: List<FieldProblem> = emptyList(),
    ): ResponseEntity<ProblemDetail> {
        val body = ProblemDetail.forStatusAndDetail(type.status, detail)
        body.title = type.title
        body.type = URI.create("$origin/problems/${type.slug}")
        if (errors.isNotEmpty()) body.setProperty("errors", errors)
        val headers = noStoreHeaders().apply { contentType = MediaType.APPLICATION_PROBLEM_JSON }
        return ResponseEntity(body, headers, type.status)
    }

    private companion object {
        val diagnostics: System.Logger = System.getLogger(GroupProblemAdvice::class.java.name)
    }
}

private fun GroupInputViolation.field(): FieldProblem = when (this) {
    GroupInputViolation.BLANK_DISPLAY_NAME ->
        FieldProblem("#/displayName", name, "Display name must not be blank.")

    GroupInputViolation.INVALID_SETTLEMENT_CURRENCY ->
        FieldProblem("#/settlementCurrency", name, "Use an uppercase ISO 4217 currency with standard minor units.")
}
