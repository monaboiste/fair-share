package com.github.monaboiste.fairshare.settlement.application.query.handler

import com.github.monaboiste.fairshare.common.queries.RegisteredQueryDispatcher
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.query.GetProposedRepayments
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import spock.lang.Specification

class GetProposedRepaymentsHandlerSpec extends Specification {
    def configuration = new SettlementTestConfiguration()

    def "querying proposals for an unknown Settlement rejects with not found"() {
        when: "proposals for an unknown Settlement are queried through dispatch"
        def result = configuration.queries.dispatch(new GetProposedRepayments(configuration.UNKNOWN_ID))

        then: "the rejection identifies the missing Settlement"
        result.getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
    }

    def "a committed Settlement absent from a lagging projection has no queryable proposals"() {
        given: "a Settlement is committed but has not reached a separate projection"
        def settlement = configuration.openSettlement("Holiday")
        def queries = RegisteredQueryDispatcher.builder()
                .register(GetProposedRepayments, new GetProposedRepaymentsHandler(new SettlementProjector())).build()

        when: "proposals are queried through that lagging projection"
        def result = queries.dispatch(new GetProposedRepayments(settlement))

        then: "not projected yet is reported as the typed not-found rejection"
        configuration.store.exists(settlement)
        result.getFailure() == new SettlementNotFound(settlement)
    }
}
