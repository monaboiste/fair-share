package com.github.monaboiste.fairshare.settlement.application.query.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.EUR
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.UNKNOWN_ID

import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import spock.lang.Specification

class GetSettlementHandlerSpec extends Specification {
    def configuration = new SettlementTestConfiguration()

    def "querying a Settlement returns its projected view"() {
        given:
        SettlementId id = configuration.openSettlement("Holiday")

        when:
        def result = configuration.viewHandler.handle(new GetSettlement(id))

        then:
        result.getSuccess() == new SettlementView(id, "Holiday", EUR, 1, [], [], [], [:])
    }

    def "querying an unknown Settlement rejects with not found"() {
        when:
        def result = configuration.viewHandler.handle(new GetSettlement(UNKNOWN_ID))

        then:
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
    }

    def "querying a committed Settlement absent from a lagging view rejects with not found"() {
        given:
        SettlementId id = configuration.openSettlement("Holiday")
        def lagging = new GetSettlementHandler(new SettlementProjector())

        when:
        def result = lagging.handle(new GetSettlement(id))

        then:
        configuration.store.exists(id)
        result.getFailure() == new SettlementNotFound(id)
    }
}
