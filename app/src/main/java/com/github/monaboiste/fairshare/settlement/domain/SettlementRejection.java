package com.github.monaboiste.fairshare.settlement.domain;

public sealed interface SettlementRejection permits IdentifierConflict, SettlementNotFound {}
