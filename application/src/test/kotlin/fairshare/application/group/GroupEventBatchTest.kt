package fairshare.application.group

import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import fairshare.domain.group.events.GroupCreated
import fairshare.domain.group.events.GroupEvent
import fairshare.domain.group.events.GroupSettlementCurrencyChanged
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class GroupEventBatchTest :
    ExpectSpec({
        val name = GroupName("Trip")
        val pln = SettlementCurrency("PLN")
        val changed = GroupSettlementCurrencyChanged(pln)
        val created = GroupCreated(name, pln)

        expect("a Group batch snapshots ordered nonempty events") {
            val mutable = mutableListOf<GroupEvent>(created, changed)
            val batch = GroupEventBatch(mutable)
            mutable.clear()
            batch.events shouldBe listOf(created, changed)
            shouldThrow<IllegalArgumentException> { GroupEventBatch(emptyList()) }
        }

        expect("creation can only be the opening event of a batch") {
            shouldThrow<IllegalArgumentException> { GroupEventBatch(listOf(changed, created)) }
            shouldThrow<IllegalArgumentException> { GroupEventBatch(listOf(created, created)) }
        }
    })
