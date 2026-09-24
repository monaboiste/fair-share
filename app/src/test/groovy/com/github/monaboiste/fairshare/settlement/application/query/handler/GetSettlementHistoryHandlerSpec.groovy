package com.github.monaboiste.fairshare.settlement.application.query.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.EUR
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.NOW
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.UNKNOWN_ID

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import spock.lang.Specification

class GetSettlementHistoryHandlerSpec extends Specification {
    def configuration = new SettlementTestConfiguration()

    def "history returns ordered committed opening and rename envelopes with metadata"() {
        given:
        SettlementId id = configuration.openSettlement("Holiday")
        configuration.renameHandler.handle(new RenameSettlement(id, new SettlementName("Mountains")))

        when:
        List<EventEnvelope<SettlementId, SettlementEvent>> history = configuration.historyHandler
            .handle(new GetSettlementHistory(id)).getSuccess()

        then:
        history*.streamId() == [id, id]
        history*.sequence() == [1L, 2L]
        history*.position() == [1L, 2L]
        history*.eventId().every { it != null }
        history[0].eventId() != history[1].eventId()
        history*.occurredAt() == [NOW, NOW]
        history*.type() == ["SettlementOpened", "SettlementRenamed"]
        history*.schemaVersion() == [1, 1]
        history*.payload() == [new SettlementOpened("Holiday", EUR), new SettlementRenamed("Mountains")]
    }

    def "history of an unknown Settlement rejects with not found"() {
        when:
        def result = configuration.historyHandler.handle(new GetSettlementHistory(UNKNOWN_ID))

        then:
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
    }
}
