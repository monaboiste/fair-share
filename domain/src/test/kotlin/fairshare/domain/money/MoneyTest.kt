package fairshare.domain.money

import fairshare.domain.validation.InvalidInputException
import fairshare.domain.validation.ValidationErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal
import java.math.BigInteger

class MoneyTest : ExpectSpec() {
    init {
        context("amounts use their currency's standard minor units") {
            withData(
                mapOf(
                    "JPY uses zero decimal places" to AmountExample("JPY", "123", "123"),
                    "PLN uses two decimal places" to AmountExample("PLN", "12.3", "12.30"),
                    "BHD uses three decimal places" to AmountExample("BHD", "1.234", "1.234"),
                    "CHF accepts one cent without cash rounding" to AmountExample("CHF", "0.01", "0.01"),
                ),
            ) { (code, entered, canonical) ->
                val currency = Currency.of(code)
                val money = Money.of(currency, BigDecimal(entered))
                val expected = BigDecimal(canonical)

                money.currency shouldBe currency
                money.amount shouldBe expected
                money.amount.scale() shouldBe expected.scale()
            }
        }

        context("excess fractional digits are rejected, including redundant zeros") {
            withData(
                mapOf(
                    "JPY rejects fractions" to ("JPY" to "1.1"),
                    "JPY rejects a redundant zero" to ("JPY" to "1.0"),
                    "PLN rejects fractions of a minor unit" to ("PLN" to "10.001"),
                    "PLN rejects a redundant third decimal place" to ("PLN" to "10.000"),
                    "BHD rejects fractions of a minor unit" to ("BHD" to "1.2345"),
                    "BHD rejects a redundant fourth decimal place" to ("BHD" to "1.2340"),
                ),
            ) { (code, entered) ->
                val failure = shouldThrow<InvalidInputException> { Money.of(Currency.of(code), BigDecimal(entered)) }
                failure.code shouldBe ValidationErrorCode.EXCESS_PRECISION
                failure.code.id shouldBe "error.validation.excess-precision"
            }
        }

        expect("an arithmetic failure raising the scale is not an excess precision failure") {
            val amount = BigDecimal(BigInteger.ONE, Int.MIN_VALUE)
            shouldThrowExactly<ArithmeticException> { Money.of(Currency.of("PLN"), amount) }
        }

        expect("money equality uses currency and canonical amount, not entered scale") {
            val amount = money("0.1")

            amount shouldBe money("0.10")
            amount shouldNotBe money("0.11")
            amount shouldNotBe money("0.10", "EUR")
        }

        expect("adding and subtracting amounts in one currency is exact") {
            val smaller = money("0.10")
            val larger = money("0.20")

            smaller + larger shouldBe money("0.30")
            smaller - larger shouldBe money("-0.10")
            larger - larger shouldBe money("0")
        }

        expect("negating an amount reverses its sign without changing currency") {
            val amount = money("1.234", "BHD")

            -amount shouldBe money("-1.234", "BHD")
            -(-amount) shouldBe amount
        }

        expect("sign checks distinguish positive, negative and zero amounts") {
            val positive = money("0.01")
            val negative = money("-0.01")
            val zero = money("0")

            positive.isPositive shouldBe true
            positive.isNegative shouldBe false
            positive.isZero shouldBe false

            negative.isPositive shouldBe false
            negative.isNegative shouldBe true
            negative.isZero shouldBe false

            zero.isPositive shouldBe false
            zero.isNegative shouldBe false
            zero.isZero shouldBe true
        }

        expect("addition and subtraction reject another currency even for zero amounts") {
            val amount = money("1.00")
            val foreignZero = money("0", "EUR")

            shouldThrow<IllegalArgumentException> { amount + foreignZero }
            shouldThrow<IllegalArgumentException> { amount - foreignZero }
        }
    }
}

private data class AmountExample(val code: String, val entered: String, val canonical: String)

private fun money(amount: String, code: String = "PLN"): Money = Money.of(Currency.of(code), BigDecimal(amount))
