package fairshare.infrastructure.rest.model

internal data class GroupCreatedResponse(
    val groupId: String,
    val displayName: String,
    val settlementCurrency: String,
) {
    @JvmField
    val participants: List<Nothing> = emptyList()
}
