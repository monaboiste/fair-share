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
import java.math.RoundingMode

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

        expect("multiplying by default accepts an exact result even with redundant trailing zeros") {
            money("1.20").multiply(BigDecimal("0.500")) shouldBe money("0.60")
        }

        expect("multiplying by default rejects a fraction of a minor unit") {
            shouldThrowExactly<ArithmeticException> { money("0.01").multiply(BigDecimal("0.5")) }
        }

        expect("multiplication can round a half-minor-unit tie up in BHD") {
            val product = money("1.235", "BHD").multiply(BigDecimal("0.1"), RoundingMode.HALF_UP)

            product shouldBe money("0.124", "BHD")
            product.amount.scale() shouldBe 3
        }

        expect("multiplication rounds a negative half-minor-unit tie away from zero") {
            money("-1.235", "BHD").multiply(BigDecimal("0.1"), RoundingMode.HALF_UP) shouldBe money("-0.124", "BHD")
        }

        expect("dividing by default accepts an exact result") {
            money("1.20").divide(BigDecimal("0.500")) shouldBe money("2.40")
        }

        expect("dividing by default rejects a non-terminating quotient") {
            shouldThrowExactly<ArithmeticException> { money("10").divide(BigDecimal("3")) }
        }

        expect("dividing by default rejects an exact quotient smaller than one minor unit") {
            shouldThrowExactly<ArithmeticException> { money("0.01").divide(BigDecimal("2")) }
        }

        expect("division can round a half-minor-unit tie up in JPY") {
            val quotient = money("5", "JPY").divide(BigDecimal("2"), RoundingMode.HALF_UP)

            quotient shouldBe money("3", "JPY")
            quotient.amount.scale() shouldBe 0
        }

        expect("division can round a negative quotient toward negative infinity") {
            money("-10").divide(BigDecimal("3"), RoundingMode.FLOOR) shouldBe money("-3.34")
        }

        expect("division rounds a negative half-minor-unit tie away from zero") {
            money("-5", "JPY").divide(BigDecimal("2"), RoundingMode.HALF_UP) shouldBe money("-3", "JPY")
        }

        expect("division rejects zero even when rounding is allowed") {
            shouldThrowExactly<ArithmeticException> { money("1").divide(BigDecimal.ZERO, RoundingMode.HALF_UP) }
        }

        context("dividing by a positive count floors the quotient and conserves the original amount") {
            withData(
                mapOf(
                    "PLN 10 divided into thirds leaves one grosz" to DivisionExample("PLN", "10", 3, "3.33", "0.01"),
                    "one grosz divided into thirds leaves the whole amount" to DivisionExample(
                        "PLN",
                        "0.01",
                        3,
                        "0",
                        "0.01",
                    ),
                    "negative PLN 10 floors to -3.34" to DivisionExample("PLN", "-10", 3, "-3.34", "0.02"),
                    "JPY floors to whole yen" to DivisionExample("JPY", "5", 2, "2", "1"),
                    "BHD floors to thousandths" to DivisionExample("BHD", "1", 3, "0.333", "0.001"),
                    "division by one leaves no remainder" to DivisionExample("PLN", "10", 1, "10", "0"),
                ),
            ) { (code, amount, count, expectedQuotient, expectedRemainder) ->
                val original = money(amount, code)
                val (quotient, remainder) = original.divideAndRemainder(count)

                quotient shouldBe money(expectedQuotient, code)
                remainder shouldBe money(expectedRemainder, code)
                quotient.multiply(count.toBigDecimal()) + remainder shouldBe original
            }
        }

        context("division with remainder requires a positive count") {
            withData(mapOf("zero count" to 0, "negative count" to -1)) { count ->
                shouldThrowExactly<IllegalArgumentException> { money("10").divideAndRemainder(count) }
            }
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

private data class DivisionExample(
    val code: String,
    val amount: String,
    val count: Int,
    val quotient: String,
    val remainder: String,
)

private fun money(amount: String, code: String = "PLN"): Money = Money.of(Currency.of(code), BigDecimal(amount))
