package fairshare.infrastructure.rest

import fairshare.infrastructure.rest.model.FieldProblem
import tools.jackson.databind.DatabindException
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.exc.UnrecognizedPropertyException
import java.net.URI

internal class GroupRequestFailure(val errors: List<FieldProblem> = emptyList()) : RuntimeException()

internal enum class FieldCode(private val detail: String) {
    REQUIRED_FIELD("A required JSON body or field is missing."),
    INVALID_TYPE("Expected a JSON object or string field, not null or another type."),
    INVALID_JSON("Expected one well-formed JSON object with unique field names."),
    UNSUPPORTED_FIELD("Field is not supported."),
    BLANK_DISPLAY_NAME("Display name must not be blank."),
    INVALID_SETTLEMENT_CURRENCY("Use an uppercase ISO 4217 currency with standard minor units."),
    ;

    fun at(pointer: String): FieldProblem = FieldProblem(pointer, name, detail)
}

internal fun unreadableBody(cause: Throwable?): FieldProblem = when (cause) {
    is UnrecognizedPropertyException -> FieldCode.UNSUPPORTED_FIELD.at(cause.pointer())

    is MismatchedInputException -> if (cause.isMalformedDocument()) {
        FieldCode.INVALID_JSON.at("#")
    } else {
        FieldCode.INVALID_TYPE.at(cause.pointer())
    }

    else -> FieldCode.INVALID_JSON.at("#")
}

private fun MismatchedInputException.isMalformedDocument(): Boolean =
    currentToken == null || originalMessage.startsWith("Trailing token (")

private fun DatabindException.pointer(): String {
    val fragment = path.joinToString(separator = "") { ref ->
        val segment = ref.propertyName?.replace("~", "~0")?.replace("/", "~1") ?: ref.index.toString()
        "/$segment"
    }
    return URI(null, null, "", null, fragment).toASCIIString()
}
