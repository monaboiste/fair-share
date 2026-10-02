package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.queries.RegisteredQueryDispatcher
import com.github.monaboiste.fairshare.netting.ProposedRepayment
import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.CancelExpense
import com.github.monaboiste.fairshare.settlement.application.command.CancelRepayment
import com.github.monaboiste.fairshare.settlement.application.command.ConfigureExchangeRate
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.application.command.RecordRepayment
import com.github.monaboiste.fairshare.settlement.application.command.RenameParticipant
import com.github.monaboiste.fairshare.settlement.application.query.GetProposedRepayments
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.ParticipantView
import com.github.monaboiste.fairshare.settlement.application.query.handler.GetProposedRepaymentsHandler
import com.github.monaboiste.fairshare.settlement.application.query.handler.GetSettlementHandler
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import com.github.monaboiste.fairshare.valuation.ExchangeRate
import java.time.LocalDate
import spock.lang.Specification

class ProposedRepaymentsQuerySpec extends Specification {
    private static final ParticipantId ADA = participant(11)
    private static final ParticipantId BOB = participant(12)
    private static final ParticipantId CAL = participant(13)
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    def configuration = new SettlementTestConfiguration()

    def "proposals instruct debtors to repay creditors without recording transfers"() {
        given: "Ada paid 20 euros shared with Bob while Cal has no financial entries"
        def settlement = withParticipants()
        expense(settlement, 21, ADA, 20, [ADA, BOB])
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "the Proposed Repayment Graph is queried twice"
        def graph = proposals(settlement)
        def repeated = proposals(settlement)

        then: "semantic vertices include Cal and the edge tells Bob to repay Ada 10 euros"
        graph.id() == settlement
        graph.currency() == configuration.EUR
        graph.version() == before.version()
        graph.participants() == [new ParticipantView(ADA, "Ada"), new ParticipantView(BOB, "Bob"), new ParticipantView(CAL, "Cal")]
        graph.proposedRepayments() == [proposal(BOB, ADA, 10)]
        preservesBalances(graph, before)
        repeated == graph
        settlementView(settlement) == before
        settlementView(settlement).repayments().isEmpty()
        history(settlement) == previousHistory
    }

    def "netting removes parallel opposite and cyclic Obligations while preserving every Balance"() {
        given: "three Participants with parallel and opposite Obligations and a financial cycle"
        def settlement = withParticipants()
        expense(settlement, 21, ADA, 12, [BOB])
        expense(settlement, 22, ADA, 3, [BOB])
        expense(settlement, 23, BOB, 4, [ADA])
        expense(settlement, 24, BOB, 7, [CAL])
        expense(settlement, 25, CAL, 2, [ADA])
        expense(settlement, 26, CAL, 9, [CAL])
        repay(settlement, 31, BOB, ADA, 2)

        when: "the current Proposed Repayment Graph is queried"
        def graph = proposals(settlement)
        def view = settlementView(settlement)

        then: "only two debtor-to-creditor instructions remain"
        view.balances() == [(ADA): Money.of(7, "EUR"), (BOB): Money.of(-2, "EUR"), (CAL): Money.of(-5, "EUR")]
        graph.proposedRepayments() == [proposal(BOB, ADA, 2), proposal(CAL, ADA, 5)]
        preservesBalances(graph, view)
    }

    def "equal Balances use Participant identity despite duplicate names and later renames"() {
        given: "equal debtors and creditors added in reverse identity order with duplicate names"
        def settlement = withParticipants()
        def dex = participant(14)
        configuration.commands.dispatch(new AddParticipant(settlement, dex, new ParticipantName("Ada"))).getSuccess()
        expense(settlement, 21, ADA, 10, [CAL])
        expense(settlement, 22, BOB, 10, [dex])
        def before = proposals(settlement)

        when: "names change to a different lexical order"
        configuration.commands.dispatch(new RenameParticipant(settlement, ADA, new ParticipantName("Zoe"))).getSuccess()
        configuration.commands.dispatch(new RenameParticipant(settlement, BOB, new ParticipantName("Ada"))).getSuccess()
        def after = proposals(settlement)

        then: "identity breaks ties and names remain current without altering the old snapshot"
        before.proposedRepayments() == [proposal(CAL, ADA, 10), proposal(dex, BOB, 10)]
        after.proposedRepayments() == before.proposedRepayments()
        before.participants()*.name() == ["Ada", "Bob", "Cal", "Ada"]
        after.participants() == [new ParticipantView(ADA, "Zoe"), new ParticipantView(BOB, "Ada"),
                               new ParticipantView(CAL, "Cal"), new ParticipantView(dex, "Ada")]
        after.version() == before.version() + 2
        preservesBalances(after, settlementView(settlement))
    }

    def "public edges are ordered by debtor then creditor identity rather than matching order"() {
        given: "the largest debtor owes two creditors and the largest creditor has the later identity"
        def settlement = withParticipants()
        def dex = participant(14)
        configuration.commands.dispatch(new AddParticipant(settlement, dex, new ParticipantName("Dex"))).getSuccess()
        expense(settlement, 21, BOB, 10, [CAL])
        expense(settlement, 22, ADA, 8, [CAL])
        expense(settlement, 23, BOB, 5, [dex])

        when: "the Proposed Repayment Graph is queried"
        def graph = proposals(settlement)

        then: "edges are explicitly ordered even when greedy matching visits Bob first"
        graph.proposedRepayments() == [proposal(CAL, ADA, 3), proposal(CAL, BOB, 15), proposal(dex, ADA, 5)]
        preservesBalances(graph, settlementView(settlement))
    }

    def "cancelled entries stay in history but only their replacements affect proposals after replay"() {
        given: "an Expense and Repayment were cancelled and replaced under new identifiers"
        def settlement = withParticipants()
        expense(settlement, 21, ADA, 20, [BOB])
        repay(settlement, 31, BOB, ADA, 7)
        configuration.commands.dispatch(new CancelExpense(settlement, new ExpenseId(new UUID(0, 21)))).getSuccess()
        configuration.commands.dispatch(new CancelRepayment(settlement, new RepaymentId(new UUID(0, 31)))).getSuccess()
        expense(settlement, 22, ADA, 12, [BOB])
        repay(settlement, 32, BOB, ADA, 3)
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "proposals are queried live and through a rebuilt projection"
        def graph = proposals(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)
        def queries = RegisteredQueryDispatcher.builder()
                .register(GetSettlement, new GetSettlementHandler(rebuilt))
                .register(GetProposedRepayments, new GetProposedRepaymentsHandler(rebuilt)).build()
        def replayed = queries.dispatch(new GetProposedRepayments(settlement)).getSuccess()
        def replayedView = queries.dispatch(new GetSettlement(settlement)).getSuccess()

        then: "the active history leaves Bob owing Ada nine euros and replay gives the same graph and Balances"
        graph.proposedRepayments() == [proposal(BOB, ADA, 9)]
        preservesBalances(graph, before)
        replayed == graph
        replayedView == before
        settlementView(settlement) == before
        history(settlement) == previousHistory
        before.expenses()*.status()*.name() == ["CANCELLED", "ACTIVE"]
        before.repayments()*.status()*.name() == ["CANCELLED", "ACTIVE"]
    }

    def "foreign Expense proposals retain frozen Shares when Exchange Rates change and the projection replays"() {
        given: "a ten dollar Expense valued at 9.50 euros with rounded equal Shares and a partial Repayment"
        def settlement = withParticipants()
        configuration.commands.dispatch(new ConfigureExchangeRate(settlement,
                ExchangeRate.of(configuration.USD, configuration.EUR, new BigDecimal("0.95")), Validity.always())).getSuccess()
        configuration.commands.dispatch(new RecordExpense(settlement, new ExpenseId(new UUID(0, 21)),
                new ExpenseDescription("Lunch"), DATE, ADA, Money.of(10, "USD"),
                new EqualShareAllocation([CAL, BOB, ADA]))).getSuccess()
        repay(settlement, 31, BOB, ADA, 1)
        def original = proposals(settlement)

        when: "a replacement Exchange Rate is configured and the projection rebuilds without Pricing"
        configuration.commands.dispatch(new ConfigureExchangeRate(settlement,
                ExchangeRate.of(configuration.USD, configuration.EUR, new BigDecimal("2")), Validity.always())).getSuccess()
        def graph = proposals(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)
        def queries = RegisteredQueryDispatcher.builder()
                .register(GetSettlement, new GetSettlementHandler(rebuilt))
                .register(GetProposedRepayments, new GetProposedRepaymentsHandler(rebuilt)).build()

        then: "the frozen cents rather than the current Exchange Rate determine Balances and proposals"
        graph.proposedRepayments() == [proposal(BOB, ADA, 2.17), proposal(CAL, ADA, 3.16)]
        graph.proposedRepayments() == original.proposedRepayments()
        graph.version() == original.version() + 1
        settlementView(settlement).balances() == [(ADA): Money.of(5.33, "EUR"), (BOB): Money.of(-2.17, "EUR"), (CAL): Money.of(-3.16, "EUR")]
        preservesBalances(graph, settlementView(settlement))
        queries.dispatch(new GetProposedRepayments(settlement)).getSuccess() == graph
        queries.dispatch(new GetSettlement(settlement)).getSuccess() == settlementView(settlement)
    }

    def "actual Repayment leaves #financialState proposals"() {
        given: "Bob owes Ada ten euros for an Expense"
        def settlement = withParticipants()
        expense(settlement, 21, ADA, 20, [ADA, BOB])

        when: "Bob records the specified actual Repayment"
        repay(settlement, 31, BOB, ADA, paid)
        def graph = proposals(settlement)
        def before = settlementView(settlement)
        def previousHistory = history(settlement)
        def repeated = proposals(settlement)

        then: "proposals reflect the remaining or reversed Balance without another recorded Repayment"
        graph.proposedRepayments() == expected
        graph.participants()*.id() == [ADA, BOB, CAL]
        preservesBalances(graph, before)
        repeated == graph
        before.repayments().size() == 1
        settlementView(settlement) == before
        history(settlement) == previousHistory

        where:
        financialState | paid | expected
        "partial"      | 4    | [proposal(BOB, ADA, 6)]
        "settled"      | 10   | []
        "overpaid"     | 15   | [proposal(ADA, BOB, 5)]
    }

    def "a Settlement with #rosterState returns an empty Proposed Repayment Graph"() {
        given: "an open Settlement with no financial history"
        def settlement = hasParticipants ? withParticipants() : configuration.openSettlement("Holiday")

        when: "proposals are queried"
        def graph = proposals(settlement)

        then: "the graph has no edges and retains exactly the current Participants"
        graph.id() == settlement
        graph.currency() == configuration.EUR
        graph.version() == settlementView(settlement).version()
        graph.participants()*.id() == expectedParticipants
        graph.proposedRepayments().isEmpty()
        preservesBalances(graph, settlementView(settlement))

        where:
        rosterState              | hasParticipants | expectedParticipants
        "no Participants"        | false           | []
        "isolated Participants"  | true            | [ADA, BOB, CAL]
    }

    private void repay(def settlement, int suffix, ParticipantId payer, ParticipantId recipient, Number amount) {
        configuration.commands.dispatch(new RecordRepayment(settlement, new RepaymentId(new UUID(0, suffix)),
                DATE, payer, recipient, Money.of(amount, "EUR"))).getSuccess()
    }

    private def withParticipants() {
        def settlement = configuration.openSettlement("Holiday")
        [Cal: CAL, Bob: BOB, Ada: ADA].each { name, id ->
            configuration.commands.dispatch(new AddParticipant(settlement, id, new ParticipantName(name))).getSuccess()
        }
        settlement
    }

    private void expense(def settlement, int suffix, ParticipantId payer, Number amount, List<ParticipantId> shares) {
        configuration.commands.dispatch(new RecordExpense(settlement, new ExpenseId(new UUID(0, suffix)),
                new ExpenseDescription("Lunch"), DATE, payer, Money.of(amount, "EUR"), new EqualShareAllocation(shares))).getSuccess()
    }

    private def proposals(def settlement) {
        configuration.queries.dispatch(new GetProposedRepayments(settlement)).getSuccess()
    }

    private def settlementView(def settlement) {
        configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()
    }

    private def history(def settlement) {
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()
    }

    private static boolean preservesBalances(def graph, def view) {
        def balances = graph.participants().collectEntries { [(it.id()): Money.zero(graph.currency())] }
        def pairs = []
        graph.proposedRepayments().each { edge ->
            assert edge.debtor() != edge.creditor()
            assert !edge.amount().isZero()
            assert !edge.amount().isNegative()
            assert edge.amount().currencyUnit() == graph.currency()
            assert view.balances()[edge.debtor()].isNegative()
            assert !view.balances()[edge.creditor()].isNegative()
            assert !view.balances()[edge.creditor()].isZero()
            balances[edge.debtor()] = balances[edge.debtor()].subtract(edge.amount())
            balances[edge.creditor()] = balances[edge.creditor()].add(edge.amount())
            pairs.add([edge.debtor(), edge.creditor()])
        }
        assert pairs.toSet().size() == pairs.size()
        assert graph.proposedRepayments().size() <= Math.max(0, view.balances().values().count { !it.isZero() } - 1)
        assert balances == view.balances()
        true
    }

    private static ProposedRepayment<ParticipantId> proposal(ParticipantId debtor, ParticipantId creditor, Number amount) {
        new ProposedRepayment<>(debtor, creditor, Money.of(amount, "EUR"))
    }

    private static ParticipantId participant(int suffix) {
        new ParticipantId(new UUID(0, suffix))
    }
}
