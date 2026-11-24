package fairshare.infrastructure.rest.error

internal interface ProblemDocumentationApi {
    fun invalidRequest(): String

    fun internalError(): String
}
