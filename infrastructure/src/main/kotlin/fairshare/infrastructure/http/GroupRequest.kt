package fairshare.infrastructure.http

import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI

internal data class CreateGroupRequest(val displayName: String, val settlementCurrency: String)

internal class GroupRequestFailure(val errors: List<FieldProblem> = emptyList()) : RuntimeException()

internal fun createGroupRequest(body: ByteArray?): CreateGroupRequest {
    val fields = readStrings(body, setOf("displayName", "settlementCurrency"))
    return CreateGroupRequest(fields.getValue("displayName"), fields.getValue("settlementCurrency"))
}

internal fun changeCurrencyRequest(body: ByteArray?): String =
    readStrings(body, setOf("settlementCurrency")).getValue("settlementCurrency")

private val mapper = JsonMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
    .build()

private fun readStrings(body: ByteArray?, fields: Set<String>): Map<String, String> {
    val json = readObject(body)
    val errors = json.propertyNames().filterNot { it in fields }.map {
        FieldProblem(pointer(it), "UNSUPPORTED_FIELD", "Field is not supported.")
    } + fields.mapNotNull { field -> validateField(field, json.get(field)) }
    if (errors.isNotEmpty()) throw GroupRequestFailure(errors)
    return fields.associateWith { json.get(it).stringValue() }
}

private fun readObject(body: ByteArray?): JsonNode {
    if (body == null) throw GroupRequestFailure(listOf(FieldProblem("#", "REQUIRED_FIELD", "A JSON body is required.")))
    val json = parseJson(body)
    if (!json.isObject) {
        throw GroupRequestFailure(listOf(FieldProblem("#", "INVALID_TYPE", "Expected a JSON object.")))
    }
    return json
}

private fun parseJson(body: ByteArray): JsonNode = try {
    mapper.readTree(body)
} catch (_: JacksonException) {
    throw GroupRequestFailure(listOf(FieldProblem("#", "INVALID_JSON", "Expected one well-formed JSON object.")))
}

private fun validateField(field: String, value: JsonNode?): FieldProblem? = when {
    value == null -> FieldProblem("#", "REQUIRED_FIELD", "Field '$field' is required.")
    !value.isString -> FieldProblem(pointer(field), "INVALID_TYPE", "Expected a JSON string.")
    else -> null
}

private fun pointer(field: String): String {
    val segment = field.replace("~", "~0").replace("/", "~1")
    return URI(null, null, "", null, "/$segment").toASCIIString()
}
