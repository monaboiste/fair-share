package com.softwarearchetypes.rules.discounting.fixture.client.rules

import com.softwarearchetypes.rules.core.predicates.RichLogicalPredicate
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContext

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

record TimeBeingCustomer(int threshold, ChronoUnit unit)
        implements RichLogicalPredicate<ClientContext> {

    static TimeBeingCustomer ofDays(int threshold) {
        new TimeBeingCustomer(threshold, ChronoUnit.DAYS)
    }

    static TimeBeingCustomer ofWeeks(int threshold) {
        new TimeBeingCustomer(threshold, ChronoUnit.WEEKS)
    }

    static TimeBeingCustomer ofMonths(int threshold) {
        new TimeBeingCustomer(threshold, ChronoUnit.MONTHS)
    }

    static TimeBeingCustomer ofYears(int threshold) {
        new TimeBeingCustomer(threshold, ChronoUnit.YEARS)
    }

    @Override
    boolean test(ClientContext clientContext) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault())

        switch (unit) {
            case [ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS, ChronoUnit.YEARS]:
                return unit.between(clientContext.firstOrder(), today) >= threshold
            default:
                return false
        }
    }
}
