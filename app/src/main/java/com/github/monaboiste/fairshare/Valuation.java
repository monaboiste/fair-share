package com.github.monaboiste.fairshare;

import com.softwarearchetypes.pricing.SimpleComponentVersion;
import com.softwarearchetypes.quantity.money.Money;

public record Valuation(Money money, ExchangeRate exchangeRate, SimpleComponentVersion componentVersion) {}
