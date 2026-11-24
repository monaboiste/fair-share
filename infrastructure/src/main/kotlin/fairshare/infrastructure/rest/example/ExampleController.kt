package fairshare.infrastructure.rest.example

import fairshare.infrastructure.rest.example.model.ExampleResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class ExampleController : ExampleApi {
    @GetMapping("/example", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun example(): ExampleResponse = ExampleResponse("test")
}
