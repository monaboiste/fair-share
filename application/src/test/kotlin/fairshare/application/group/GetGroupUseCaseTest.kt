package fairshare.application.group

import fairshare.domain.group.GroupCreated
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.GroupSettlementCurrencyChanged
import fairshare.domain.group.SettlementCurrency
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class GetGroupUseCaseTest :
    BehaviorSpec({
        given("a Group store containing recorded settings") {
            val id = GroupId("weekend")
            val revision = GroupRevision("opaque")
            val name = checkNotNull(GroupName.parse("Trip"))
            val pln = checkNotNull(SettlementCurrency.parse("PLN"))
            val eur = checkNotNull(SettlementCurrency.parse("EUR"))
            val store = RecordingGroupStore().apply {
                streams[id] =
                    GroupStream(revision, listOf(GroupCreated(name, pln), GroupSettlementCurrencyChanged(eur)))
            }
            val get = GetGroupUseCase(store)

            then("the addressed Group reflects the full ordered history and store revision") {
                val snapshot = get.execute(id)
                snapshot.id shouldBe id
                snapshot.revision shouldBe revision
                snapshot.group.name shouldBe name
                snapshot.group.settlementCurrency shouldBe eur
            }

            then("reading a different identifier fails without writing") {
                shouldThrow<GroupFailure.NotFound> { get.execute(GroupId("missing")) }
                store.appendCount shouldBe 0
            }
        }
    })
