package fairshare.domain.group

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class GroupTest :
    ExpectSpec({
        val name = checkNotNull(GroupName.parse("Weekend trip"))
        val pln = checkNotNull(SettlementCurrency.parse("PLN"))
        val eur = checkNotNull(SettlementCurrency.parse("EUR"))
        val usd = checkNotNull(SettlementCurrency.parse("USD"))
        val created = Group.create(name, pln)

        expect("configuration is rebuilt from all recorded facts in order") {
            val original = Group.replay(listOf(created))
            val change = original.changeSettlementCurrency(eur)
            val laterChange = original.apply(change).changeSettlementCurrency(usd)
            val current = Group.replay(listOf(created, change, laterChange))
            original.settlementCurrency shouldBe pln
            change.currency shouldBe eur
            current.name shouldBe name
            current.settlementCurrency shouldBe usd
            current.settlementCurrencyLocked shouldBe false
            current.participants shouldBe emptyList()
        }

        expect("a stream without exactly one opening creation cannot produce a Group") {
            shouldThrow<IllegalStateException> { Group.replay(emptyList()) }
            shouldThrow<IllegalStateException> { Group.replay(listOf(GroupSettlementCurrencyChanged(pln))) }
            shouldThrow<IllegalStateException> { Group.replay(listOf(created, created)) }
        }

        expect("Group settings reject blank names and unsupported currency codes") {
            listOf("", " \t", "\u2003").forEach { GroupName.parse(it) shouldBe null }
            name.value shouldBe "Weekend trip"
            GroupId("weekend").value shouldBe "weekend"
            pln.code shouldBe "PLN"
            listOf("pln", "EUR ", "ZZZ", "", "XXX").forEach { SettlementCurrency.parse(it) shouldBe null }
            checkNotNull(SettlementCurrency.parse("JPY")).minorUnits shouldBe 0
            checkNotNull(SettlementCurrency.parse("PLN")).minorUnits shouldBe 2
            checkNotNull(SettlementCurrency.parse("KWD")).minorUnits shouldBe 3
        }
    })
