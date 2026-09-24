package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import java.util.Optional;

public record GetSettlement(SettlementId id) implements SettlementQuery<Optional<SettlementView>> {}
