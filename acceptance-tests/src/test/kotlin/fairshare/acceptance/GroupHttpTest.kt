package fairshare.acceptance

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.net.http.HttpClient

class GroupHttpTest :
    BehaviorSpec({
        given("a running Fair Share backend") {
            then("a public caller can configure a Group, read it from another instance, and refresh after restart") {
                val database = tempdir().resolve("group-api.db")

                HttpClient.newHttpClient().use { client ->
                    val (location, changed) = startBackend(database).use { backend ->
                        val origin = backend.origin
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

                        startBackend(database).use { peer ->
                            val peerUrl = "${peer.origin}$path"
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

                    startBackend(database).use { restarted ->
                        val group = "${restarted.origin}$location"
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
