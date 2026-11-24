package fairshare.infrastructure.rest.example

import fairshare.infrastructure.rest.example.model.ExampleResponse

internal interface ExampleApi {
    fun example(): ExampleResponse
}
