package com.github.monaboiste.fairshare.settlement.application.command.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.NOW
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.UNKNOWN_ID

import com.github.monaboiste.fairshare.common.events.CommitResult
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository
import spock.lang.Specification

class RenameSettlementHandlerSpec extends Specification {
    def configuration = new SettlementTestConfiguration()

    def "renaming a Settlement returns the next version and committed rename envelope"() {
        given: "a Settlement called Holiday is opened"
        SettlementId id = configuration.openSettlement("Holiday")
        def opening = configuration.store.load(id).first()

        when: "it is renamed to Mountains"
        def result = configuration.renameHandler.handle(new RenameSettlement(id, new SettlementName("Mountains")))

        then: "the commit carries a new rename at the next version, stamped with the current time"
        def commit = result.getSuccess()
        commit.streamId() == id
        commit.version() == 2
        commit.events().size() == 1
        commit.events().first().streamId() == id
        commit.events().first().sequence() == 2
        commit.events().first().eventId() != null
        commit.events().first().eventId() != opening.eventId()
        commit.events().first().occurredAt() == NOW
        commit.events().first().payload() == new SettlementRenamed("Mountains")
    }

    def "renaming to the current name succeeds without an envelope or version change"() {
        given: "a Settlement that has already been renamed to Mountains"
        SettlementId id = configuration.openSettlement("Holiday")
        configuration.renameHandler.handle(new RenameSettlement(id, new SettlementName("Mountains")))

        when: "it is renamed to Mountains again"
        def result = configuration.renameHandler.handle(new RenameSettlement(id, new SettlementName("Mountains")))

        then: "it succeeds at the same version and nothing new is saved"
        result.getSuccess().streamId() == id
        result.getSuccess().events().empty
        result.getSuccess().version() == 2
        configuration.store.load(id).size() == 2
    }

    def "renaming an unknown Settlement rejects without creating a stream"() {
        when: "a Settlement that does not exist is renamed"
        def result = configuration.renameHandler.handle(new RenameSettlement(UNKNOWN_ID, new SettlementName("Mountains")))

        then: "it is rejected as not found and no stream is created"
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
        !configuration.store.exists(UNKNOWN_ID)
    }

    def "stale Settlement rename fails optimistic concurrency and preserves the winner"() {
        given: "a handler holds an outdated copy of a Settlement after it was renamed to Winner"
        SettlementId id = configuration.openSettlement("Holiday")
        def stale = configuration.repository.findById(id).orElseThrow()
        configuration.renameHandler.handle(new RenameSettlement(id, new SettlementName("Winner")))
        SettlementRepository outdated = new SettlementRepository() {
            Optional<Settlement> findById(SettlementId lookupId) { Optional.of(stale) }
            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) { configuration.repository.save(settlement) }
        }

        when: "the outdated handler renames it to Loser"
        new RenameSettlementHandler(outdated).handle(new RenameSettlement(id, new SettlementName("Loser")))

        then: "the save fails on a version conflict and the Settlement keeps the winning name"
        thrown(VersionConflictException)
        configuration.projector.findById(id).orElseThrow().name() == "Winner"
    }
}
