package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.commands.RegisteredCommandDispatcher
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RecordRepayment
import com.github.monaboiste.fairshare.settlement.application.command.handler.RecordRepaymentHandler
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.RepaymentRecorded
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import java.time.LocalDate
import spock.lang.Specification

class RepaymentReplaySpec extends Specification {
    private static final ParticipantId ADA = participant(11)
    private static final ParticipantId BOB = participant(12)
    private static final ParticipantId CAL = participant(13)
    private static final ParticipantId UNKNOWN = participant(14)
    private static final RepaymentId REPAYMENT = repayment(21)
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    def configuration = new SettlementTestConfiguration()

    def "replay rejects inconsistent Repayment history: #caseName through #route"() {
        given: "a recorded Repayment and a copied history with an inconsistent later event"
        def settlement = withParticipants()
        configuration.commands.dispatch(
                new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR")))
        def copied = new InMemoryEventStore()
        def pending = history(settlement).collect {
            new PendingEvent<SettlementEvent>(it.eventId(), it.payload(), it.occurredAt())
        }
        pending.add(new PendingEvent<SettlementEvent>(EventId.random(), inconsistent, configuration.NOW))
        copied.append(settlement, 0, pending)
        def rebuilt = new SettlementProjector()
        def commands = RegisteredCommandDispatcher.builder()
                .register(RecordRepayment, new RecordRepaymentHandler(
                        new EventSourcedSettlementRepository(copied, configuration.CLOCK))).build()

        when: "the saved history is replayed"
        if (route == "projection") {
            rebuilt.rebuild(copied)
        } else {
            commands.dispatch(new RecordRepayment(settlement, repayment(23), DATE, BOB, ADA, Money.of(1, "EUR")))
        }

        then: "replay rejects the inconsistent history"
        thrown(IllegalStateException)
        rebuilt.findById(settlement).isEmpty()

        where:
        caseName            | inconsistent                                                                 | route
        "zero amount"       | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.zero("EUR"))      | "projection"
        "zero amount"       | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.zero("EUR"))      | "command"
        "negative amount"   | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.of(-1, "EUR"))    | "projection"
        "negative amount"   | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.of(-1, "EUR"))    | "command"
        "foreign currency"  | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.of(1, "USD"))     | "projection"
        "foreign currency"  | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.of(1, "USD"))     | "command"
        "excess precision"  | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.of(1.001, "EUR")) | "projection"
        "excess precision"  | new RepaymentRecorded(repayment(22), DATE, BOB, ADA, Money.of(1.001, "EUR")) | "command"
        "duplicate"         | new RepaymentRecorded(REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR"))        | "projection"
        "duplicate"         | new RepaymentRecorded(REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR"))        | "command"
        "unknown payer"     | new RepaymentRecorded(repayment(22), DATE, UNKNOWN, ADA, Money.of(1, "EUR")) | "projection"
        "unknown payer"     | new RepaymentRecorded(repayment(22), DATE, UNKNOWN, ADA, Money.of(1, "EUR")) | "command"
        "unknown recipient" | new RepaymentRecorded(repayment(22), DATE, BOB, UNKNOWN, Money.of(1, "EUR")) | "projection"
        "unknown recipient" | new RepaymentRecorded(repayment(22), DATE, BOB, UNKNOWN, Money.of(1, "EUR")) | "command"
        "self-directed"     | new RepaymentRecorded(repayment(22), DATE, BOB, BOB, Money.of(1, "EUR"))     | "projection"
        "self-directed"     | new RepaymentRecorded(repayment(22), DATE, BOB, BOB, Money.of(1, "EUR"))     | "command"
        "payer removed"     | new ParticipantRemoved(BOB)                                                  | "projection"
        "payer removed"     | new ParticipantRemoved(BOB)                                                  | "command"
        "recipient removed" | new ParticipantRemoved(ADA)                                                  | "projection"
        "recipient removed" | new ParticipantRemoved(ADA)                                                  | "command"
    }

    private def withParticipants() {
        def settlement = configuration.openSettlement("Holiday")
        [ADA, BOB, CAL].each { id ->
            configuration.commands.dispatch(new AddParticipant(settlement, id, new ParticipantName("Participant ${id.value()}")))
        }
        settlement
    }

    private def history(def settlement) {
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()
    }

    private static ParticipantId participant(int suffix) {
        new ParticipantId(new UUID(0, suffix))
    }

    private static RepaymentId repayment(int suffix) {
        new RepaymentId(new UUID(0, suffix))
    }
}
