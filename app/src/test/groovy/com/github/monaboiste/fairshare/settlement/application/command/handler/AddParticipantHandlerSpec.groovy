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
        given: "a Settlement called Holiday is opened"
        SettlementId id = configuration.openSettlement("Holiday")

        when: "Ada is added as a Participant"
        def commit = configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Ada"))).getSuccess()

        then: "the commit carries one Participant addition at the next version, stamped with the current time"
        commit.streamId() == id
        commit.version() == 2
        commit.events().size() == 1
        commit.events().first().streamId() == id
        commit.events().first().sequence() == 2
        commit.events().first().occurredAt() == NOW
        commit.events().first().payload() == new ParticipantAdded(ADA, "Ada")
    }

    def "identical Participant retry succeeds without committing"() {
        given: "Ada has already been added to an open Settlement"
        SettlementId id = configuration.openSettlement("Holiday")
        def command = new AddParticipant(id, ADA, new ParticipantName("Ada"))
        configuration.addHandler.handle(command)

        when: "the same addition is sent again"
        def retry = configuration.addHandler.handle(command).getSuccess()

        then: "it succeeds at the same version and nothing new is saved"
        retry.streamId() == id
        retry.version() == 2
        retry.events().empty
        configuration.store.load(id).size() == 2
    }

    def "reusing a Participant identifier with different add data rejects"() {
        given: "Ada has already been added to an open Settlement"
        SettlementId id = configuration.openSettlement("Holiday")
        configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Ada")))

        when: "the same Participant identifier is added again under the name Alex"
        def result = configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Alex")))

        then: "the addition is rejected as an identifier conflict and nothing new is saved"
        result.getFailure() == new ParticipantIdentifierConflict(id, ADA)
        configuration.store.load(id).size() == 2
    }

    def "adding to an unknown Settlement rejects without creating a stream"() {
        when: "a Participant is added to a Settlement that does not exist"
        def result = configuration.addHandler.handle(new AddParticipant(UNKNOWN_ID, ADA, new ParticipantName("Ada")))

        then: "it is rejected as not found and no stream is created"
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
        !configuration.store.exists(UNKNOWN_ID)
    }

    def "stale Participant addition fails optimistic concurrency and preserves the winner"() {
        given: "a handler holds an outdated copy of a Settlement after the Participant was added as Winner"
        SettlementId id = configuration.openSettlement("Holiday")
        def stale = configuration.repository.findById(id).orElseThrow()
        configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Winner")))
        SettlementRepository outdated = new SettlementRepository() {
            Optional<Settlement> findById(SettlementId lookupId) { Optional.of(stale) }
            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) { configuration.repository.save(settlement) }
        }

        when: "the outdated handler adds the same Participant as Loser"
        new AddParticipantHandler(outdated).handle(new AddParticipant(id, ADA, new ParticipantName("Loser")))

        then: "the save fails on a version conflict and the Participant keeps the winning name"
        thrown(VersionConflictException)
        configuration.projector.findById(id).orElseThrow().participants().first().name() == "Winner"
    }
}
