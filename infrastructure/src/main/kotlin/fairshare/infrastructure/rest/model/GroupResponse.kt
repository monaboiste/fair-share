package fairshare.infrastructure.rest.model

internal data class GroupResponse(
    val groupId: String,
    val displayName: String,
    val settlementCurrency: String,
    val settlementCurrencyLocked: Boolean,
    val participants: List<Nothing>,
)
