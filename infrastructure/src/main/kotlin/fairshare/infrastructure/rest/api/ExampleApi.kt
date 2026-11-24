package fairshare.infrastructure.rest.api

import fairshare.infrastructure.rest.model.ExampleResponse

internal interface ExampleApi {
    fun example(): ExampleResponse
}
