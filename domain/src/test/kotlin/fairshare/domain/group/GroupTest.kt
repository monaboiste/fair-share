package fairshare.domain.group

import fairshare.domain.group.events.GroupCreated
import fairshare.domain.group.events.GroupSettlementCurrencyChanged
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class GroupTest :
    ExpectSpec({
        val name = GroupName("Weekend trip")
        val pln = SettlementCurrency("PLN")
        val eur = SettlementCurrency("EUR")
        val usd = SettlementCurrency("USD")
        val created = Group.create(name, pln)

        expect("creation emits the name and initial Settlement currency") {
            created shouldBe GroupCreated(name, pln)
            Group.replay(listOf(created)).settlementCurrency shouldBe pln
        }

        expect("replay applies currency changes in recorded order without altering the prior Group") {
            val original = Group.replay(listOf(created))
            val change = original.changeSettlementCurrency(eur)
            val laterChange = original.apply(change).changeSettlementCurrency(usd)
            val current = Group.replay(listOf(created, change, laterChange))

            original.settlementCurrency shouldBe pln
            change shouldBe GroupSettlementCurrencyChanged(eur)
            original.apply(change).settlementCurrency shouldBe eur
            current.name shouldBe name
            current.settlementCurrency shouldBe usd
        }

        expect("a stream without exactly one opening creation cannot produce a Group") {
            shouldThrow<IllegalStateException> { Group.replay(emptyList()) }
            shouldThrow<IllegalStateException> { Group.replay(listOf(GroupSettlementCurrencyChanged(pln))) }
            shouldThrow<IllegalStateException> { Group.replay(listOf(created, created)) }
        }
    })
