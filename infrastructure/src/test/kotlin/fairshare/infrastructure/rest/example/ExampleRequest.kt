package fairshare.infrastructure.rest.example

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

internal data class ExampleRequest(
    @field:NotNull
    @field:Size(min = 1)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val message: String? = null,
    val tags: List<String> = emptyList(),
)
