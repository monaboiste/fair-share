package com.github.monaboiste.fairshare.settlement.application.command.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.NOW
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.UNKNOWN_ID

import com.github.monaboiste.fairshare.common.events.CommitResult
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository
import spock.lang.Specification

class RemoveParticipantHandlerSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0L, 12L))
    def configuration = new SettlementTestConfiguration()

    def "removing a Participant commits an ordered envelope"() {
        given: "Ada is a Participant in an open Settlement"
        SettlementId id = withParticipant()

        when: "Ada is removed"
        def commit = configuration.removeParticipantHandler.handle(new RemoveParticipant(id, ADA)).getSuccess()

        then: "the commit carries one Participant removal at the next version, stamped with the current time"
        commit.streamId() == id
        commit.version() == 3
        commit.events().size() == 1
        commit.events().first().streamId() == id
        commit.events().first().sequence() == 3
        commit.events().first().occurredAt() == NOW
        commit.events().first().payload() == new ParticipantRemoved(ADA)
    }

    def "removing an absent Participant rejects without committing"() {
        given: "Ada is the only Participant in an open Settlement"
        SettlementId id = withParticipant()

        when: "Bob, who was never added, is removed"
        def result = configuration.removeParticipantHandler.handle(new RemoveParticipant(id, BOB))

        then: "the removal is rejected as not found and nothing new is saved"
        result.getFailure() == new ParticipantNotFound(id, BOB)
        configuration.store.load(id).size() == 2
    }

    def "removing from an unknown Settlement rejects without creating a stream"() {
        when: "a Participant is removed from a Settlement that does not exist"
        def result = configuration.removeParticipantHandler.handle(new RemoveParticipant(UNKNOWN_ID, ADA))

        then: "it is rejected as not found and no stream is created"
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
        !configuration.store.exists(UNKNOWN_ID)
    }

    def "stale Participant removal fails optimistic concurrency and preserves the winner"() {
        given: "a handler holds an outdated copy of a Settlement after Bob was added"
        SettlementId id = withParticipant()
        def stale = configuration.repository.findById(id).orElseThrow()
        configuration.addHandler.handle(new AddParticipant(id, BOB, new ParticipantName("Bob")))
        SettlementRepository outdated = new SettlementRepository() {
            Optional<Settlement> findById(SettlementId lookupId) { Optional.of(stale) }

            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) {
                configuration.repository.save(settlement)
            }
        }

        when: "the outdated handler removes Ada"
        new RemoveParticipantHandler(outdated).handle(new RemoveParticipant(id, ADA))

        then: "the save fails on a version conflict and both Ada and Bob remain Participants"
        thrown(VersionConflictException)
        configuration.projector.findById(id).orElseThrow().participants()*.id() == [ADA, BOB]
    }

    private SettlementId withParticipant() {
        SettlementId id = configuration.openSettlement("Holiday")
        configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Ada")))
        id
    }
}
