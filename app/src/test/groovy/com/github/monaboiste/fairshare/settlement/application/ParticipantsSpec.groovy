package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameParticipant
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import spock.lang.Specification

class ParticipantsSpec extends Specification {
    def configuration = new SettlementTestConfiguration()
    private static final ParticipantId ADA = new ParticipantId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
    private static final ParticipantId BOB = new ParticipantId(UUID.fromString("00000000-0000-0000-0000-000000000012"))

    def "participants with the same display name retain separate identifiers and addition order"() {
        given: "an open Settlement called Holiday"
        def settlementId = configuration.openSettlement("Holiday")

        when: "two Participants both named Alex are added"
        def first = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        def second = configuration.commands.dispatch(new AddParticipant(settlementId, BOB, new ParticipantName("Alex")))
        def view = configuration.queries.dispatch(new GetSettlement(settlementId)).getSuccess()

        then: "each keeps a separate identity and they are listed in the order they were added"
        first.getSuccess().events()*.payload() == [new ParticipantAdded(ADA, "Alex")]
        second.getSuccess().version() == 3
        view.participants()*.id() == [ADA, BOB]
        view.participants()*.name() == ["Alex", "Alex"]
    }

    def "identical add retries are idempotent and changed original data conflicts"() {
        given: "a Settlement with Participant Alex"
        def settlementId = configuration.openSettlement("Holiday")
        configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))

        when: "Alex is added again unchanged, and then again under a different name"
        def retry = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        def conflict = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Ada")))

        then: "the unchanged retry records nothing, while the changed one conflicts and is not stored"
        retry.getSuccess().events().empty
        retry.getSuccess().version() == 2
        conflict.getFailure() == new ParticipantIdentifierConflict(settlementId, ADA)
        configuration.store.load(settlementId).size() == 2
    }

    def "renaming retains identity and position; removal retires the identifier but leaves history"() {
        given: "a Settlement with two Participants both named Alex"
        def settlementId = configuration.openSettlement("Holiday")
        configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        configuration.commands.dispatch(new AddParticipant(settlementId, BOB, new ParticipantName("Alex")))

        when: "the first is renamed to Ada twice, then removed and added again"
        def renamed = configuration.commands.dispatch(new RenameParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def unchanged = configuration.commands.dispatch(new RenameParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def renamedView = configuration.queries.dispatch(new GetSettlement(settlementId)).getSuccess()
        def removed = configuration.commands.dispatch(new RemoveParticipant(settlementId, ADA))
        def retry = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        def view = configuration.queries.dispatch(new GetSettlement(settlementId)).getSuccess()
        def history = configuration.queries.dispatch(new GetSettlementHistory(settlementId)).getSuccess()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "the rename keeps identity and position, removal leaves history, and the retired identity is not revived"
        renamed.getSuccess().events()*.payload() == [new ParticipantRenamed(ADA, "Ada")]
        unchanged.getSuccess().events().empty
        unchanged.getSuccess().version() == 4
        renamedView.participants()*.id() == [ADA, BOB]
        renamedView.participants()*.name() == ["Ada", "Alex"]
        removed.getSuccess().events()*.payload() == [new ParticipantRemoved(ADA)]
        retry.getSuccess().events().empty
        retry.getSuccess().version() == 5
        view.participants()*.id() == [BOB]
        history*.sequence() == [1L, 2L, 3L, 4L, 5L]
        history*.payload()[-1] == new ParticipantRemoved(ADA)
        rebuilt.findById(settlementId).orElseThrow() == view
    }

    def "a retired identifier conflicts on changed original data and cannot be renamed or removed"() {
        given: "a Settlement where Participant Alex was added and then removed"
        def settlementId = configuration.openSettlement("Holiday")
        configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        configuration.commands.dispatch(new RemoveParticipant(settlementId, ADA))

        when: "the retired Participant is added with other data, renamed or removed"
        def conflict = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def rename = configuration.commands.dispatch(new RenameParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def remove = configuration.commands.dispatch(new RemoveParticipant(settlementId, ADA))

        then: "adding conflicts, renaming and removing find no such Participant, and nothing is stored"
        conflict.getFailure() == new ParticipantIdentifierConflict(settlementId, ADA)
        rename.getFailure() == new ParticipantNotFound(settlementId, ADA)
        remove.getFailure() == new ParticipantNotFound(settlementId, ADA)
        configuration.store.load(settlementId).size() == 3
    }

    def "unknown settlement and participant reject without committing"() {
        given: "an open Settlement without Participants"
        def settlementId = configuration.openSettlement("Holiday")

        expect: "changes to an unknown Settlement or Participant are rejected and nothing is stored"
        configuration.commands.dispatch(new AddParticipant(configuration.UNKNOWN_ID, ADA, new ParticipantName("Ada")))
                .getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
        configuration.commands.dispatch(new RenameParticipant(configuration.UNKNOWN_ID, ADA, new ParticipantName("Ada")))
                .getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
        configuration.commands.dispatch(new RemoveParticipant(configuration.UNKNOWN_ID, ADA))
                .getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
        configuration.commands.dispatch(new RenameParticipant(settlementId, ADA, new ParticipantName("Ada")))
                .getFailure() == new ParticipantNotFound(settlementId, ADA)
        configuration.commands.dispatch(new RemoveParticipant(settlementId, ADA))
                .getFailure() == new ParticipantNotFound(settlementId, ADA)
        configuration.store.load(settlementId).size() == 1
    }
}
