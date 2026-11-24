package fairshare.application.group

import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class GetGroupUseCaseTest :
    BehaviorSpec({
        given("a repository with one readable Group") {
            val id = GroupId("weekend")
            val view = GroupView(id, GroupName("Trip"), SettlementCurrency("EUR"), GroupRevision("opaque"))
            val get = GetGroupUseCase(
                object : GroupViewRepository {
                    override fun findById(id: GroupId): GroupView? = view.takeIf { it.id == id }
                },
            )

            then("reading the addressed Group returns its view and revision") {
                get.execute(id) shouldBe view
            }

            then("reading a different identifier fails as not found") {
                shouldThrow<GroupFailure.NotFound> { get.execute(GroupId("missing")) }
            }
        }
    })
