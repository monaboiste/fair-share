package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlement
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import spock.lang.Specification

class SettlementApplicationSpec extends Specification {
    def configuration = new SettlementTestConfiguration()

    def "dispatching open and rename exposes the view and ordered history"() {
        when:
        def opened = configuration.commands.dispatch(new OpenSettlement(new SettlementName("Holiday"), configuration.EUR))
        SettlementId id = opened.getSuccess().streamId()
        def renamed = configuration.commands.dispatch(new RenameSettlement(id, new SettlementName("Mountains")))
        def view = configuration.queries.dispatch(new GetSettlement(id))
        def history = configuration.queries.dispatch(new GetSettlementHistory(id))

        then:
        opened.getSuccess().version() == 1
        renamed.getSuccess().version() == 2
        view.getSuccess() == new SettlementView(id, "Mountains", configuration.EUR, 2, [], [], [], [:])
        history.getSuccess()*.sequence() == [1L, 2L]
    }

    def "rebuilding from stored commands reproduces the live view"() {
        given:
        SettlementId id = configuration.commands.dispatch(new OpenSettlement(new SettlementName("Holiday"), configuration.EUR))
            .getSuccess().streamId()
        configuration.commands.dispatch(new RenameSettlement(id, new SettlementName("Mountains")))
        def rebuilt = new SettlementProjector()

        when:
        rebuilt.rebuild(configuration.store)

        then:
        rebuilt.findById(id).orElseThrow() == configuration.queries.dispatch(new GetSettlement(id)).getSuccess()
    }
}
