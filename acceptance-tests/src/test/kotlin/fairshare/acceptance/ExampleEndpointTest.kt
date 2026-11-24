package fairshare.acceptance

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ExampleEndpointTest :
    BehaviorSpec({
        given("Fair Share is available") {
            `when`("I request the example message") {
                then("I receive the message") {
                    val baseUrl = System.getenv("FAIR_SHARE_BASE_URL") ?: "http://localhost:8080"
                    val request = HttpRequest.newBuilder(URI.create("${baseUrl.trimEnd('/')}/example"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build()

                    HttpClient.newHttpClient().use { client ->
                        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                        val mapper = JsonMapper()

                        response.statusCode() shouldBe 200
                        mapper.readTree(response.body()) shouldBe mapper.readTree("""{"message":"test"}""")
                    }
                }
            }
        }
    })
