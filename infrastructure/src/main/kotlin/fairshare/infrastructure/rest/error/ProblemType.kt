package fairshare.infrastructure.rest.error

import org.springframework.http.HttpStatus

internal enum class ProblemType(
    val path: String,
    val status: HttpStatus,
    val title: String,
    val messageKey: String,
    val defaultDetail: String,
) {
    INVALID_REQUEST(
        "/problems/invalid-request",
        HttpStatus.BAD_REQUEST,
        "Invalid request",
        "error.validation.invalid-request",
        "The request is invalid.",
    ),
    INTERNAL_ERROR(
        "/problems/internal-error",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "error.internal.unknown",
        "An unexpected error occurred.",
    ),
}
