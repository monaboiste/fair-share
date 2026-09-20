package com.github.monaboiste.fairshare.rules.discounting.fixture.client.rules

import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.rules.core.predicates.RichLogicalPredicate
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientContext
import groovy.transform.ImmutableOptions

@ImmutableOptions(knownImmutableClasses = Money)
record ExpensesRule(Money minAmount) implements RichLogicalPredicate<ClientContext> {

    static ExpensesRule of(Money minAmount) {
        new ExpensesRule(minAmount)
    }

    @Override
    boolean test(ClientContext clientContext) {
        clientContext.totalExpenses().isGreaterThanOrEqualTo(minAmount)
    }
}
