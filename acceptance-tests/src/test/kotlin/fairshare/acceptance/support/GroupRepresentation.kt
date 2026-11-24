package fairshare.acceptance.support

import tools.jackson.databind.JsonNode

internal data class GroupRepresentation(
    val id: String,
    val displayName: String,
    val settlementCurrency: String,
    val participants: List<JsonNode>,
)
