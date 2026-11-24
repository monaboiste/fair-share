package fairshare.infrastructure.rest.error.model

internal data class FieldError(val pointer: String, val code: String, val detail: String)
