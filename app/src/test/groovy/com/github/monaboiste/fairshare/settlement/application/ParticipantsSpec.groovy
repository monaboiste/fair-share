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
        given:
        def settlementId = configuration.openSettlement("Holiday")

        when:
        def first = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        def second = configuration.commands.dispatch(new AddParticipant(settlementId, BOB, new ParticipantName("Alex")))
        def view = configuration.queries.dispatch(new GetSettlement(settlementId)).getSuccess()

        then:
        first.getSuccess().events()*.payload() == [new ParticipantAdded(ADA, "Alex")]
        second.getSuccess().version() == 3
        view.participants()*.id() == [ADA, BOB]
        view.participants()*.name() == ["Alex", "Alex"]
    }

    def "identical add retries are idempotent and changed original data conflicts"() {
        given:
        def settlementId = configuration.openSettlement("Holiday")
        configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))

        when:
        def retry = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        def conflict = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Ada")))

        then:
        retry.getSuccess().events().empty
        retry.getSuccess().version() == 2
        conflict.getFailure() == new ParticipantIdentifierConflict(settlementId, ADA)
        configuration.store.load(settlementId).size() == 2
    }

    def "renaming retains identity and position; removal retires the identifier but leaves history"() {
        given:
        def settlementId = configuration.openSettlement("Holiday")
        configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        configuration.commands.dispatch(new AddParticipant(settlementId, BOB, new ParticipantName("Alex")))

        when:
        def renamed = configuration.commands.dispatch(new RenameParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def unchanged = configuration.commands.dispatch(new RenameParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def renamedView = configuration.queries.dispatch(new GetSettlement(settlementId)).getSuccess()
        def removed = configuration.commands.dispatch(new RemoveParticipant(settlementId, ADA))
        def retry = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        def view = configuration.queries.dispatch(new GetSettlement(settlementId)).getSuccess()
        def history = configuration.queries.dispatch(new GetSettlementHistory(settlementId)).getSuccess()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then:
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
        given:
        def settlementId = configuration.openSettlement("Holiday")
        configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Alex")))
        configuration.commands.dispatch(new RemoveParticipant(settlementId, ADA))

        when:
        def conflict = configuration.commands.dispatch(new AddParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def rename = configuration.commands.dispatch(new RenameParticipant(settlementId, ADA, new ParticipantName("Ada")))
        def remove = configuration.commands.dispatch(new RemoveParticipant(settlementId, ADA))

        then:
        conflict.getFailure() == new ParticipantIdentifierConflict(settlementId, ADA)
        rename.getFailure() == new ParticipantNotFound(settlementId, ADA)
        remove.getFailure() == new ParticipantNotFound(settlementId, ADA)
        configuration.store.load(settlementId).size() == 3
    }

    def "unknown settlement and participant reject without committing"() {
        given:
        def settlementId = configuration.openSettlement("Holiday")

        expect:
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
