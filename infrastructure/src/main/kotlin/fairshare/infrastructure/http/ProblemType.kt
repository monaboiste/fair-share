package fairshare.infrastructure.http

import org.springframework.http.HttpStatus

internal enum class ProblemType(val slug: String, val status: HttpStatus, val title: String, val detail: String) {
    INVALID_REQUEST(
        "invalid-request",
        HttpStatus.BAD_REQUEST,
        "Invalid request",
        "The request did not meet the expected JSON or If-Match format.",
    ),
    GROUP_NOT_FOUND(
        "group-not-found",
        HttpStatus.NOT_FOUND,
        "Group not found",
        "No Group exists with the supplied identifier.",
    ),
    GROUP_VERSION_MISMATCH(
        "group-version-mismatch",
        HttpStatus.PRECONDITION_FAILED,
        "Group version mismatch",
        "The Group revision did not match. This request did not commit. Refresh the Group before resubmitting.",
    ),
    INVALID_GROUP(
        "invalid-group",
        HttpStatus.UNPROCESSABLE_CONTENT,
        "Invalid group",
        "The Group settings did not meet the business rules.",
    ),
    PRECONDITION_REQUIRED(
        "precondition-required",
        HttpStatus.PRECONDITION_REQUIRED,
        "Precondition required",
        "Supply the current strong Group ETag in If-Match.",
    ),
    INTERNAL_ERROR(
        "internal-error",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "The backend could not complete the request. If submitting a write, refresh the Group before resubmitting.",
    ),
}

internal data class FieldProblem(val pointer: String, val code: String, val detail: String)
