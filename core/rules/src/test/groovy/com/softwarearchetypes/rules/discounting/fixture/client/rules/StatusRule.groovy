package com.softwarearchetypes.rules.discounting.fixture.client.rules

import com.softwarearchetypes.rules.core.predicates.RichLogicalPredicate
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContext
import com.softwarearchetypes.rules.discounting.fixture.client.ClientStatus

record StatusRule(ClientStatus status) implements RichLogicalPredicate<ClientContext> {

    static StatusRule of(ClientStatus status) {
        new StatusRule(status)
    }

    @Override
    boolean test(ClientContext clientContext) {
        clientContext.status() == status
    }
}
