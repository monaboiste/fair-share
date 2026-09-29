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
        given: "a Settlement called Holiday is opened in euros"
        SettlementId id = configuration.openSettlement("Holiday")

        when: "the Settlement is queried"
        def result = configuration.viewHandler.handle(new GetSettlement(id))

        then: "its view shows the name, currency and version, with no Participants, Expenses, Obligations or Balances"
        result.getSuccess() == new SettlementView(id, "Holiday", EUR, 1, [], [], [], [:])
    }

    def "querying an unknown Settlement rejects with not found"() {
        when: "a Settlement that does not exist is queried"
        def result = configuration.viewHandler.handle(new GetSettlement(UNKNOWN_ID))

        then: "it is rejected as not found"
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
    }

    def "querying a committed Settlement absent from a lagging view rejects with not found"() {
        given: "a Settlement is opened but a lagging projection has not seen it yet"
        SettlementId id = configuration.openSettlement("Holiday")
        def lagging = new GetSettlementHandler(new SettlementProjector())

        when: "the Settlement is queried through the lagging projection"
        def result = lagging.handle(new GetSettlement(id))

        then: "it is rejected as not found even though the Settlement is saved"
        configuration.store.exists(id)
        result.getFailure() == new SettlementNotFound(id)
    }
}
