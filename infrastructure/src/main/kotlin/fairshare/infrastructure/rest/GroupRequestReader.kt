package fairshare.infrastructure.rest

import fairshare.infrastructure.rest.model.ChangeGroupSettlementCurrencyRequest
import fairshare.infrastructure.rest.model.CreateGroupRequest
import fairshare.infrastructure.rest.model.FieldProblem
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI

internal class GroupRequestFailure(val errors: List<FieldProblem> = emptyList()) : RuntimeException()

@Component
internal class GroupRequestReader(private val mapper: JsonMapper, private val validator: Validator) {
    fun create(body: ByteArray?): CreateGroupRequest {
        val fields = readStrings(body, setOf("displayName", "settlementCurrency"))
        return CreateGroupRequest(fields.getValue("displayName"), fields.getValue("settlementCurrency"))
    }

    fun changeCurrency(body: ByteArray?): ChangeGroupSettlementCurrencyRequest {
        val fields = readStrings(body, setOf("settlementCurrency"))
        return ChangeGroupSettlementCurrencyRequest(fields.getValue("settlementCurrency"))
    }

    private fun readStrings(body: ByteArray?, fields: Set<String>): Map<String, String> {
        val json = readObject(body)
        val errors = mutableListOf<FieldProblem>()
        for (field in json.propertyNames()) {
            if (field !in fields) errors += FieldProblem(pointer(field), "UNSUPPORTED_FIELD", "Field is not supported.")
        }
        val strings = mutableMapOf<String, String>()
        for (field in fields) {
            val node = json[field]
            val string = node?.stringValue(null)
            when {
                validator.validate(RequiredJsonField(node)).isNotEmpty() ->
                    errors += FieldProblem("#", "REQUIRED_FIELD", "Field '$field' is required.")

                string == null ->
                    errors += FieldProblem(pointer(field), "INVALID_TYPE", "Expected a JSON string.")

                else -> strings[field] = string
            }
        }
        if (errors.isNotEmpty()) throw GroupRequestFailure(errors)
        return strings
    }

    private fun readObject(body: ByteArray?): JsonNode {
        if (body == null) {
            throw GroupRequestFailure(listOf(FieldProblem("#", "REQUIRED_FIELD", "A JSON body is required.")))
        }
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
}

private class RequiredJsonField(@get:NotNull val node: JsonNode?)

private fun pointer(field: String): String {
    val segment = field.replace("~", "~0").replace("/", "~1")
    return URI(null, null, "", null, "/$segment").toASCIIString()
}
