package fairshare.domain.money

import fairshare.domain.validation.InvalidInputException
import fairshare.domain.validation.ValidationErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class CurrencyTest : ExpectSpec() {
    init {
        expect("a supported ISO code identifies a settlement currency") {
            Currency.of("PLN").code shouldBe "PLN"
        }

        context("unsupported settlement currencies are rejected without normalization") {
            withData(
                mapOf(
                    "unknown code" to "ZZZ",
                    "lowercase code" to "pln",
                    "mixed case code" to "PlN",
                    "leading space" to " PLN",
                    "trailing space" to "PLN ",
                    "numeric ISO code" to "985",
                    "wrong length" to "PLNN",
                    "empty code" to "",
                    "no currency" to "XXX",
                    "precious metal" to "XAU",
                ),
            ) { code ->
                val failure = shouldThrow<InvalidInputException> { Currency.of(code) }
                failure.code shouldBe ValidationErrorCode.UNSUPPORTED_CURRENCY
                failure.code.id shouldBe "error.validation.unsupported-currency"
            }
        }
    }
}
