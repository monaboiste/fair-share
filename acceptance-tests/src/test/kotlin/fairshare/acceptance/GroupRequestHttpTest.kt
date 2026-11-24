package fairshare.acceptance

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import tools.jackson.databind.JsonNode
import java.net.http.HttpClient
import java.net.http.HttpResponse

class GroupRequestHttpTest :
    BehaviorSpec({
        given("the real MVC JSON and Problem Details configuration") {
            then(
                "the single parser rejects duplicate and trailing fields and Bean Validation detects missing settings",
            ) {
                HttpClient.newHttpClient().use { client ->
                    startBackend(tempdir().resolve("validation.db")).use { backend ->
                        val groups = "${backend.origin}/groups"
                        client.request("POST", groups, """{"displayName":"Trip","displayName":"Again"}""")
                            .problem(400, "invalid-request", "/groups").fieldCodes() shouldBe
                            listOf("#" to "INVALID_JSON")
                        client.request("POST", groups, """{"displayName":"Trip","settlementCurrency":"PLN"} true""")
                            .problem(400, "invalid-request", "/groups").fieldCodes() shouldBe
                            listOf("#" to "INVALID_JSON")
                        client.request(
                            "POST",
                            groups,
                            "{",
                            accept = "application/json, application/problem+json;charset=ISO-8859-1",
                        ).problem(400, "invalid-request", "/groups").fieldCodes() shouldBe
                            listOf("#" to "INVALID_JSON")
                        client.request("POST", groups, """{"displayName":false,"extra/key":true}""")
                            .problem(400, "invalid-request", "/groups").fieldCodes() shouldBe
                            listOf(
                                "#/extra~1key" to "UNSUPPORTED_FIELD",
                                "#/displayName" to "INVALID_TYPE",
                                "#" to "REQUIRED_FIELD",
                            )
                        client.request("POST", groups, """{"displayName":" ","settlementCurrency":"eur"}""")
                            .problem(422, "invalid-group", "/groups").fieldCodes() shouldBe
                            listOf(
                                "#/displayName" to "BLANK_DISPLAY_NAME",
                                "#/settlementCurrency" to "INVALID_SETTLEMENT_CURRENCY",
                            )
                    }
                }
            }

            then("header, shape and business checks run in order, and failed requests leave the ETag unchanged") {
                HttpClient.newHttpClient().use { client ->
                    startBackend(tempdir().resolve("priority.db")).use { backend ->
                        val created = client.request(
                            "POST",
                            "${backend.origin}/groups",
                            """{"displayName":"Trip","settlementCurrency":"PLN"}""",
                        )
                        created.statusCode() shouldBe 201
                        val path = created.header("Location")
                        val tag = created.header("ETag")
                        val currencyPath = "$path/currency"
                        val currencyUrl = "${backend.origin}$currencyPath"
                        client.request("PATCH", currencyUrl, "{").problem(428, "precondition-required", currencyPath)
                        client.request("PATCH", currencyUrl, "{", "W/$tag")
                            .problem(400, "invalid-request", currencyPath).has("errors") shouldBe false
                        client.request("PATCH", currencyUrl, "{", "\"stale\"")
                            .problem(400, "invalid-request", currencyPath).fieldCodes() shouldBe
                            listOf("#" to "INVALID_JSON")
                        val invalidCurrency = """{"settlementCurrency":"eur"}"""
                        client.request("PATCH", currencyUrl, invalidCurrency, "\"stale\"")
                            .problem(412, "group-version-mismatch", currencyPath)
                        client.request("PATCH", currencyUrl, invalidCurrency, tag)
                            .problem(422, "invalid-group", currencyPath).fieldCodes() shouldBe
                            listOf("#/settlementCurrency" to "INVALID_SETTLEMENT_CURRENCY")
                        client.request("GET", "${backend.origin}$path").shouldMatchGroup(created)
                    }
                }
            }
        }
    })

private fun HttpResponse<String>.problem(status: Int, suffix: String, path: String): JsonNode {
    statusCode() shouldBe status
    header("Content-Type") shouldBe "application/problem+json"
    header("Cache-Control") shouldBe "no-store"
    val json = mapper.readTree(body())
    json["type"].stringValue() shouldBe "https://reference.example.test/problems/$suffix"
    json["status"].asInt() shouldBe status
    json["instance"].stringValue() shouldBe path
    json.has("properties") shouldBe false
    return json
}

private fun JsonNode.fieldCodes(): List<Pair<String, String>> =
    this["errors"].valueStream().map { it["pointer"].stringValue() to it["code"].stringValue() }.toList()
