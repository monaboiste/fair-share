package fairshare.infrastructure.rest.controller

import fairshare.infrastructure.rest.api.ExampleApi
import fairshare.infrastructure.rest.model.ExampleResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class ExampleController : ExampleApi {
    @GetMapping("/example", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun example(): ExampleResponse = ExampleResponse("test")
}
