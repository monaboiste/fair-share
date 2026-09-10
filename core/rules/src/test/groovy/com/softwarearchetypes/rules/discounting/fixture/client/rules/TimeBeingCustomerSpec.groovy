package com.softwarearchetypes.rules.discounting.fixture.client.rules

import com.softwarearchetypes.quantity.money.Money
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContext
import com.softwarearchetypes.rules.discounting.fixture.client.ClientStatus

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import spock.lang.Specification

class TimeBeingCustomerSpec extends Specification {

    def "checks whole #unit since the first order"() {
        given:
        def firstOrder = LocalDate.now(ZoneId.systemDefault()).minus(elapsed, unit)
        def client = new ClientContext(
                UUID.randomUUID(),
                ClientStatus.STANDARD,
                Money.zero("PLN"),
                firstOrder
        )

        expect:
        rule.test(client) == eligible

        where:
        unit              | elapsed | rule                          || eligible
        ChronoUnit.DAYS   | 10      | TimeBeingCustomer.ofDays(10) || true
        ChronoUnit.DAYS   | 9       | TimeBeingCustomer.ofDays(10)  || false
        ChronoUnit.WEEKS  | 3       | TimeBeingCustomer.ofWeeks(3)  || true
        ChronoUnit.WEEKS  | 2       | TimeBeingCustomer.ofWeeks(3)  || false
        ChronoUnit.MONTHS | 4       | TimeBeingCustomer.ofMonths(4) || true
        ChronoUnit.MONTHS | 3       | TimeBeingCustomer.ofMonths(4) || false
        ChronoUnit.YEARS  | 5       | TimeBeingCustomer.ofYears(5)  || true
        ChronoUnit.YEARS  | 4       | TimeBeingCustomer.ofYears(5)  || false
    }
}
