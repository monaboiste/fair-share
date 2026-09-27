package com.github.monaboiste.fairshare.settlement.application.command.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.NOW
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.UNKNOWN_ID

import com.github.monaboiste.fairshare.common.events.CommitResult
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository
import spock.lang.Specification

class AddParticipantHandlerSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    def configuration = new SettlementTestConfiguration()

    def "adding a Participant commits an ordered envelope"() {
        given:
        SettlementId id = configuration.openSettlement("Holiday")

        when:
        def commit = configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Ada"))).getSuccess()

        then:
        commit.streamId() == id
        commit.version() == 2
        commit.events().size() == 1
        commit.events().first().streamId() == id
        commit.events().first().sequence() == 2
        commit.events().first().occurredAt() == NOW
        commit.events().first().payload() == new ParticipantAdded(ADA, "Ada")
    }

    def "identical Participant retry succeeds without committing"() {
        given:
        SettlementId id = configuration.openSettlement("Holiday")
        def command = new AddParticipant(id, ADA, new ParticipantName("Ada"))
        configuration.addHandler.handle(command)

        when:
        def retry = configuration.addHandler.handle(command).getSuccess()

        then:
        retry.streamId() == id
        retry.version() == 2
        retry.events().empty
        configuration.store.load(id).size() == 2
    }

    def "reusing a Participant identifier with different add data rejects"() {
        given:
        SettlementId id = configuration.openSettlement("Holiday")
        configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Ada")))

        when:
        def result = configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Alex")))

        then:
        result.getFailure() == new ParticipantIdentifierConflict(id, ADA)
        configuration.store.load(id).size() == 2
    }

    def "adding to an unknown Settlement rejects without creating a stream"() {
        when:
        def result = configuration.addHandler.handle(new AddParticipant(UNKNOWN_ID, ADA, new ParticipantName("Ada")))

        then:
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
        !configuration.store.exists(UNKNOWN_ID)
    }

    def "stale Participant addition fails optimistic concurrency and preserves the winner"() {
        given:
        SettlementId id = configuration.openSettlement("Holiday")
        def stale = configuration.repository.findById(id).orElseThrow()
        configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Winner")))
        SettlementRepository outdated = new SettlementRepository() {
            Optional<Settlement> findById(SettlementId lookupId) { Optional.of(stale) }
            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) { configuration.repository.save(settlement) }
        }

        when:
        new AddParticipantHandler(outdated).handle(new AddParticipant(id, ADA, new ParticipantName("Loser")))

        then:
        thrown(VersionConflictException)
        configuration.projector.findById(id).orElseThrow().participants().first().name() == "Winner"
    }
}
