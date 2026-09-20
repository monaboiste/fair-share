package com.github.monaboiste.fairshare.rules.discounting.fixture.client

import com.github.monaboiste.fairshare.quantity.money.Money
import groovy.transform.ImmutableOptions

import java.time.LocalDate

@ImmutableOptions(knownImmutableClasses = Money)
record ClientContext(UUID id, ClientStatus status, Money totalExpenses, LocalDate firstOrder) {}
