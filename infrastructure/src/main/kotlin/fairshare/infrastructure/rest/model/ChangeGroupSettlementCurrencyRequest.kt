package fairshare.infrastructure.rest.model

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.validation.constraints.NotNull

internal data class ChangeGroupSettlementCurrencyRequest(
    @field:NotNull
    @param:JsonSetter(nulls = Nulls.FAIL)
    val settlementCurrency: String? = null,
)
