package com.github.monaboiste.fairshare.settlement.application.command.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.NOW
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.UNKNOWN_ID

import com.github.monaboiste.fairshare.common.events.CommitResult
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameParticipant
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository
import spock.lang.Specification

class RenameParticipantHandlerSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    def configuration = new SettlementTestConfiguration()

    def "renaming a Participant commits an ordered envelope"() {
        given: "Ada is a Participant in an open Settlement"
        SettlementId id = withParticipant()

        when: "Ada is renamed to Alex"
        def commit = configuration.renameParticipantHandler.handle(
            new RenameParticipant(id, ADA, new ParticipantName("Alex"))).getSuccess()

        then: "the commit carries one Participant rename at the next version, stamped with the current time"
        commit.streamId() == id
        commit.version() == 3
        commit.events().size() == 1
        commit.events().first().streamId() == id
        commit.events().first().sequence() == 3
        commit.events().first().occurredAt() == NOW
        commit.events().first().payload() == new ParticipantRenamed(ADA, "Alex")
    }

    def "renaming to the current name succeeds without committing"() {
        given: "Ada has already been renamed to Alex"
        SettlementId id = withParticipant()
        def command = new RenameParticipant(id, ADA, new ParticipantName("Alex"))
        configuration.renameParticipantHandler.handle(command)

        when: "the same rename is sent again"
        def retry = configuration.renameParticipantHandler.handle(command).getSuccess()

        then: "it succeeds at the same version and nothing new is saved"
        retry.streamId() == id
        retry.version() == 3
        retry.events().empty
        configuration.store.load(id).size() == 3
    }

    def "renaming an unknown Participant rejects without committing"() {
        given: "an open Settlement without any Participants"
        SettlementId id = configuration.openSettlement("Holiday")

        when: "a Participant is renamed to Alex"
        def result = configuration.renameParticipantHandler.handle(new RenameParticipant(id, ADA, new ParticipantName("Alex")))

        then: "the rename is rejected as not found and nothing new is saved"
        result.getFailure() == new ParticipantNotFound(id, ADA)
        configuration.store.load(id).size() == 1
    }

    def "renaming in an unknown Settlement rejects without creating a stream"() {
        when: "a Participant is renamed in a Settlement that does not exist"
        def result = configuration.renameParticipantHandler.handle(
            new RenameParticipant(UNKNOWN_ID, ADA, new ParticipantName("Alex")))

        then: "it is rejected as not found and no stream is created"
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
        !configuration.store.exists(UNKNOWN_ID)
    }

    def "stale Participant rename fails optimistic concurrency and preserves the winner"() {
        given: "a handler holds an outdated copy of a Settlement after the Participant was renamed to Winner"
        SettlementId id = withParticipant()
        def stale = configuration.repository.findById(id).orElseThrow()
        configuration.renameParticipantHandler.handle(new RenameParticipant(id, ADA, new ParticipantName("Winner")))
        SettlementRepository outdated = new SettlementRepository() {
            Optional<Settlement> findById(SettlementId lookupId) { Optional.of(stale) }
            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) { configuration.repository.save(settlement) }
        }

        when: "the outdated handler renames the Participant to Loser"
        new RenameParticipantHandler(outdated).handle(new RenameParticipant(id, ADA, new ParticipantName("Loser")))

        then: "the save fails on a version conflict and the Participant keeps the winning name"
        thrown(VersionConflictException)
        configuration.projector.findById(id).orElseThrow().participants().first().name() == "Winner"
    }

    private SettlementId withParticipant() {
        SettlementId id = configuration.openSettlement("Holiday")
        configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Ada")))
        id
    }
}
