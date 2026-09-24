package com.github.monaboiste.fairshare.settlement.application.query.handler;

import com.github.monaboiste.fairshare.common.queries.QueryHandler;
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementViews;
import java.util.Optional;

public final class GetSettlementHandler implements QueryHandler<GetSettlement, Optional<SettlementView>> {
    private final SettlementViews views;

    public GetSettlementHandler(SettlementViews views) {
        this.views = views;
    }

    @Override
    public Optional<SettlementView> handle(GetSettlement query) {
        return views.findById(query.id());
    }
}
