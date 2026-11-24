package fairshare.infrastructure.rest.controller

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class ExampleControllerTest :
    ExpectSpec({
        expect("the example endpoint returns the example message") {
            val response = ExampleController().example()

            response.message shouldBe "test"
        }
    })
