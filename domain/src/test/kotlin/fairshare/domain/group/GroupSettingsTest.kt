package fairshare.domain.group

import fairshare.domain.group.exception.InvalidGroupNameException
import fairshare.domain.group.exception.InvalidSettlementCurrencyException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class GroupSettingsTest :
    ExpectSpec({
        expect("Group names reject only blank input and keep the entered name") {
            listOf("", " \t", "\u2003").forEach { name ->
                shouldThrow<InvalidGroupNameException> { GroupName(name) }
            }
            GroupName(" Weekend trip ").value shouldBe " Weekend trip "
        }

        expect("Settlement currency uses standard minor units, including zero-decimal currencies") {
            mapOf("JPY" to 0, "XCG" to 2, "CHF" to 2, "KWD" to 3).forEach { (code, minorUnits) ->
                val currency = SettlementCurrency(code)
                currency.code shouldBe code
                currency.minorUnits shouldBe minorUnits
            }
        }

        expect("Settlement currency rejects unknown, noncanonical and pseudo-currency codes") {
            listOf("ZZZ", "pln", "EUR ", "", "XXX", "XAU").forEach { code ->
                shouldThrow<InvalidSettlementCurrencyException> { SettlementCurrency(code) }
            }
        }

        expect("new Group identities are nonblank and independent") {
            val first = GroupId.random()
            first.value.isNotBlank() shouldBe true
            (first == GroupId.random()) shouldBe false
        }
    })
