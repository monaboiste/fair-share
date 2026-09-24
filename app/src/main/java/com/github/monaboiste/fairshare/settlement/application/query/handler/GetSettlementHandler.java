package com.github.monaboiste.fairshare.settlement.application.query.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.queries.QueryHandler;
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementViews;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;

/**
 * Reads the current Settlement view from the projection.
 *
 * <p>Succeeds with the projected view or rejects with {@link SettlementNotFound} when it is absent. With asynchronous
 * delivery, not found can also mean not projected yet. Projection and store failures propagate as exceptions.
 */
public final class GetSettlementHandler implements QueryHandler<GetSettlement, SettlementRejection, SettlementView> {
    private final SettlementViews views;

    public GetSettlementHandler(SettlementViews views) {
        this.views = views;
    }

    @Override
    public Result<SettlementRejection, SettlementView> handle(GetSettlement query) {
        return views.findById(query.id())
                .map(view -> Result.<SettlementRejection, SettlementView>success(view))
                .orElseGet(() -> Result.failure(new SettlementNotFound(query.id())));
    }
}
