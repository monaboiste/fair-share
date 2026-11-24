package fairshare.acceptance.group

import fairshare.acceptance.support.FairShareClient
import fairshare.acceptance.support.FairShareTestEnvironment
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GroupManagementTest :
    BehaviorSpec({
        var environment: FairShareTestEnvironment? = null
        lateinit var client: FairShareClient

        beforeSpec {
            val running = FairShareTestEnvironment(tempdir().toPath())
            environment = running
            client = running.awaitClient()
        }

        afterSpec {
            environment?.close()
        }

        given("I am organizing a trip with friends") {
            `when`("I create a Group settling expenses in PLN") {
                then("I can access my newly created Group") {
                    val created = client.createGroup("Weekend trip", "PLN")
                    created.status shouldBe 201
                    created.location shouldBe "/groups/${created.group.id}"

                    val retrieved = client.getGroup(created.group.id)
                    retrieved.status shouldBe 200
                    retrieved.group shouldBe created.group
                    retrieved.group.displayName shouldBe "Weekend trip"
                    retrieved.group.settlementCurrency shouldBe "PLN"
                    retrieved.group.participants shouldBe emptyList()
                    retrieved.revision shouldBe created.revision
                }
            }
        }

        given("my Group settles expenses in PLN") {
            `when`("I change its Settlement currency to EUR") {
                then("my Group uses EUR for future settlements") {
                    val created = client.createGroup("Weekend trip", "PLN")

                    val changed = client.changeSettlementCurrency(created.group.id, created.revision, "EUR")
                    changed.status shouldBe 200
                    changed.group.settlementCurrency shouldBe "EUR"
                    changed.revision shouldNotBe created.revision

                    val retrieved = client.getGroup(created.group.id)
                    retrieved.group shouldBe changed.group
                    retrieved.revision shouldBe changed.revision
                }
            }
        }

        given("someone else has updated my Group") {
            `when`("I submit a change based on outdated Group details") {
                then("I cannot overwrite their changes unknowingly") {
                    val created = client.createGroup("Weekend trip", "PLN")
                    val updated = client.changeSettlementCurrency(created.group.id, created.revision, "EUR")
                    updated.status shouldBe 200

                    val rejected = client.changeSettlementCurrency(created.group.id, created.revision, "USD")
                    rejected.status shouldBe 412

                    val retrieved = client.getGroup(created.group.id)
                    retrieved.group shouldBe updated.group
                    retrieved.revision shouldBe updated.revision
                }
            }
        }

        given("I manage an existing Group") {
            `when`("I submit an unsupported Settlement currency") {
                then("my Group keeps its current settings") {
                    val created = client.createGroup("Weekend trip", "PLN")

                    val rejected = client.changeSettlementCurrency(created.group.id, created.revision, "XYZ")
                    rejected.status shouldBe 422
                    rejected.problemType shouldBe "https://reference.example.test/problems/invalid-group"

                    val retrieved = client.getGroup(created.group.id)
                    retrieved.group shouldBe created.group
                    retrieved.revision shouldBe created.revision
                }
            }
        }
    })
