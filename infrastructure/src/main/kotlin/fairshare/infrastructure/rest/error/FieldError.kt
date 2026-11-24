package fairshare.infrastructure.rest.error

internal data class FieldError(val pointer: String, val code: String, val detail: String)
