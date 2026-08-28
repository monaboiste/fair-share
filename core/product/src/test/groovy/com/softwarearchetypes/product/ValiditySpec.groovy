package com.softwarearchetypes.product

import java.time.LocalDate
import spock.lang.Specification

class ValiditySpec extends Specification {

    static class FactoryMethods extends Specification {

        def "should create validity from"() {
            given:
            LocalDate from = LocalDate.of(2024, 1, 1)

            when:
            Validity validity = Validity.from(from)

            then:
            from == validity.from()
            validity.to() == null
        }

        def "should create validity until"() {
            given:
            LocalDate to = LocalDate.of(2024, 12, 31)

            when:
            Validity validity = Validity.until(to)

            then:
            validity.from() == null
            to == validity.to()
        }

        def "should create validity between"() {
            given:
            LocalDate from = LocalDate.of(2024, 1, 1)
            LocalDate to = LocalDate.of(2024, 12, 31)

            when:
            Validity validity = Validity.between(from, to)

            then:
            from == validity.from()
            to == validity.to()
        }

        def "should create validity always"() {
            when:
            Validity validity = Validity.always()

            then:
            validity.from() == null
            validity.to() == null
        }

        def "should allow same from and to"() {
            given:
            LocalDate date = LocalDate.of(2024, 6, 15)

            when:
            Validity validity = Validity.between(date, date)

            then:
            date == validity.from()
            date == validity.to()
        }

        def "should reject from after to"() {
            given:
            LocalDate from = LocalDate.of(2024, 12, 31)
            LocalDate to = LocalDate.of(2024, 1, 1)

            when:
            Validity.between(from, to)

            then:
            thrown(IllegalArgumentException)
        }
    }

    static class IsValidAtSpec extends Specification {

        def "should determine validity within bounded range"() {
            given:
            Validity validity = Validity.between(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)
            )

            when:
            boolean valid = validity.isValidAt(date)

            then:
            valid == expected

            where:
            date                              | expected
            LocalDate.of(2023, 12, 31)        | false
            LocalDate.of(2024, 1, 1)          | true
            LocalDate.of(2024, 6, 15)         | true
            LocalDate.of(2024, 12, 31)        | true
            LocalDate.of(2025, 1, 1)          | false
        }

        def "should determine validity after from date"() {
            given:
            Validity validity = Validity.from(LocalDate.of(2024, 1, 1))

            when:
            boolean valid = validity.isValidAt(date)

            then:
            valid == expected

            where:
            date                              | expected
            LocalDate.of(2023, 12, 31)        | false
            LocalDate.of(2024, 1, 1)          | true
            LocalDate.of(2024, 6, 15)         | true
            LocalDate.of(2100, 12, 31)        | true
        }

        def "should determine validity before to date"() {
            given:
            Validity validity = Validity.until(LocalDate.of(2024, 12, 31))

            when:
            boolean valid = validity.isValidAt(date)

            then:
            valid == expected

            where:
            date                              | expected
            LocalDate.of(1900, 1, 1)          | true
            LocalDate.of(2024, 6, 15)         | true
            LocalDate.of(2024, 12, 31)        | true
            LocalDate.of(2025, 1, 1)          | false
        }

        def "should always be valid with no boundaries"() {
            given:
            Validity validity = Validity.always()

            when:
            boolean valid = validity.isValidAt(date)

            then:
            valid

            where:
            date << [
                    LocalDate.of(1900, 1, 1),
                    LocalDate.of(2024, 6, 15),
                    LocalDate.of(2100, 12, 31)
            ]
        }

        def "should not be valid for null date"() {
            given:
            Validity validity = Validity.always()

            when:
            boolean valid = validity.isValidAt(null)

            then:
            !valid
        }

        def "should be valid only on single day"() {
            given:
            LocalDate singleDay = LocalDate.of(2024, 6, 15)
            Validity validity = Validity.between(singleDay, singleDay)

            when:
            boolean valid = validity.isValidAt(date)

            then:
            valid == expected

            where:
            date                              | expected
            LocalDate.of(2024, 6, 14)         | false
            LocalDate.of(2024, 6, 15)         | true
            LocalDate.of(2024, 6, 16)         | false
        }
    }

    static class EqualitySpec extends Specification {

        def "should be equal with same boundaries"() {
            given:
            Validity validity1 = Validity.between(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)
            )
            Validity validity2 = Validity.between(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)
            )

            when:
            boolean equal = validity1 == validity2
            boolean hashCodesEqual = validity1.hashCode() == validity2.hashCode()

            then:
            equal
            hashCodesEqual
        }

        def "should be equal for always"() {
            given:
            Validity validity1 = Validity.always()
            Validity validity2 = Validity.always()

            when:
            boolean equal = validity1 == validity2
            boolean hashCodesEqual = validity1.hashCode() == validity2.hashCode()

            then:
            equal
            hashCodesEqual
        }

        def "should not be equal with different boundaries"() {
            given:
            Validity validity1 = Validity.between(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)
            )
            Validity validity2 = Validity.between(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 12, 31)
            )

            when:
            boolean equal = validity1.equals(validity2)

            then:
            !equal
        }
    }

    static class ToStringSpec extends Specification {

        def "should format always as always"() {
            given:
            Validity validity = Validity.always()

            when:
            String formatted = validity.toString()

            then:
            "always" == formatted
        }

        def "should format from date only"() {
            given:
            Validity validity = Validity.from(LocalDate.of(2024, 1, 1))

            when:
            String formatted = validity.toString()

            then:
            "from 2024-01-01" == formatted
        }

        def "should format to date only"() {
            given:
            Validity validity = Validity.until(LocalDate.of(2024, 12, 31))

            when:
            String formatted = validity.toString()

            then:
            "until 2024-12-31" == formatted
        }

        def "should format both dates"() {
            given:
            Validity validity = Validity.between(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)
            )

            when:
            String formatted = validity.toString()

            then:
            "2024-01-01 to 2024-12-31" == formatted
        }
    }
}
