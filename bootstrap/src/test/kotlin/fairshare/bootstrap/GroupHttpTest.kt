package fairshare.bootstrap

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.springframework.boot.builder.SpringApplicationBuilder
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GroupHttpTest :
    BehaviorSpec({
        given("a running Fair Share backend") {
            then("a public caller can configure a Group, read it from another instance, and refresh after restart") {
                val database = tempdir().resolve("group-api.db")
                val config = mapOf(
                    "server.port" to "0",
                    "spring.main.banner-mode" to "off",
                    "logging.level.root" to "WARN",
                    "fair-share.database-url" to "jdbc:sqlite:$database",
                    "fair-share.problem-origin" to "https://reference.example.test",
                )
                fun start() = SpringApplicationBuilder(MainApplication::class.java).properties(config).run()

                HttpClient.newHttpClient().use { client ->
                    val (location, changed) = start().use { backend ->
                        val origin = "http://localhost:${backend.environment.getRequiredProperty("local.server.port")}"
                        val created = client.request(
                            "POST",
                            "$origin/groups",
                            """{"displayName":"Weekend trip","settlementCurrency":"PLN"}""",
                        )
                        created.statusCode() shouldBe 201
                        val path = created.header("Location")
                        val initialTag = created.header("ETag")
                        initialTag.matches(Regex(""""[^"]+"""")) shouldBe true
                        val group = mapper.readTree(created.body())
                        path shouldBe "/groups/${group["groupId"].stringValue()}"
                        group["displayName"].stringValue() shouldBe "Weekend trip"
                        group["settlementCurrency"].stringValue() shouldBe "PLN"
                        group["settlementCurrencyLocked"].asBoolean() shouldBe false
                        group["participants"].isArray shouldBe true
                        group["participants"].size() shouldBe 0
                        client.request("GET", "$origin$path").shouldMatchGroup(created)

                        start().use { peer ->
                            val peerPort = peer.environment.getRequiredProperty("local.server.port")
                            val peerUrl = "http://localhost:$peerPort$path"
                            client.request("GET", peerUrl).shouldMatchGroup(created)
                            val updated = client.request(
                                "PATCH",
                                "$origin$path/currency",
                                """{"settlementCurrency":"EUR"}""",
                                initialTag,
                            )
                            updated.statusCode() shouldBe 200
                            (updated.header("ETag") == initialTag) shouldBe false
                            mapper.readTree(updated.body())["settlementCurrency"].stringValue() shouldBe "EUR"
                            client.request("GET", peerUrl).shouldMatchGroup(updated)
                            path to updated
                        }
                    }

                    start().use { restarted ->
                        val port = restarted.environment.getRequiredProperty("local.server.port")
                        val group = "http://localhost:$port$location"
                        client.request("GET", group).shouldMatchGroup(changed)
                        val rejected = client.request("PATCH", "$group/currency", """{"settlementCurrency":"JPY"}""")
                        rejected.statusCode() shouldBe 428
                        rejected.header("Content-Type") shouldBe "application/problem+json"
                        mapper.readTree(rejected.body())["type"].stringValue() shouldBe
                            "https://reference.example.test/problems/precondition-required"
                    }
                }
            }
        }
    })

private val mapper = JsonMapper()

private fun HttpClient.request(
    method: String,
    url: String,
    json: String = "",
    tag: String? = null,
): HttpResponse<String> {
    val request = HttpRequest.newBuilder(URI(url))
        .header("Content-Type", "application/json")
        .apply { if (tag != null) header("If-Match", tag) }
        .method(method, HttpRequest.BodyPublishers.ofString(json))
        .build()
    return send(request, HttpResponse.BodyHandlers.ofString())
}

private fun HttpResponse<String>.header(name: String): String = headers().firstValue(name).orElseThrow()

private fun HttpResponse<String>.shouldMatchGroup(previous: HttpResponse<String>) {
    statusCode() shouldBe 200
    header("ETag") shouldBe previous.header("ETag")
    mapper.readTree(body()) shouldBe mapper.readTree(previous.body())
}
