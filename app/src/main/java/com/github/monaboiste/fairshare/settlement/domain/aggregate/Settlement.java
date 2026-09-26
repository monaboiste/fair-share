package com.github.monaboiste.fairshare.settlement.domain.aggregate;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateFactory;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateRoot;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.EmptyShareAllocation;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseIdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.MissingExchangeRate;
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveExpenseAmount;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantReferenced;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.Share;
import com.github.monaboiste.fairshare.settlement.domain.ShareAllocation;
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import com.github.monaboiste.fairshare.valuation.ValuationEngine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public final class Settlement extends AggregateRoot<SettlementId, SettlementEvent> {
    private final SettlementId id;
    private @Nullable String name;
    private @Nullable CurrencyUnit currency;
    private final Map<ParticipantId, Participant> participants = new HashMap<>();
    private final Map<ExpenseId, ExpenseRecorded> expenses = new HashMap<>();

    private Settlement(SettlementId id, Clock clock) {
        super(clock);
        this.id = id;
    }

    public static AggregateFactory<SettlementId, Settlement> factory(Clock clock) {
        return id -> new Settlement(id, clock);
    }

    public static Settlement open(SettlementName name, CurrencyUnit currency, Clock clock) {
        Settlement settlement = new Settlement(SettlementId.random(), clock);
        settlement.register(new SettlementOpened(name.value(), currency));
        return settlement;
    }

    public void rename(SettlementName name) {
        if (!name.value().equals(this.name)) {
            register(new SettlementRenamed(name.value()));
        }
    }

    public Result<ParticipantIdentifierConflict, ParticipantId> addParticipant(
            ParticipantId participantId, ParticipantName name) {
        Participant participant = participants.get(participantId);
        if (participant != null) {
            return participant.isAddedAs(name)
                    ? Result.success(participantId)
                    : Result.failure(new ParticipantIdentifierConflict(id, participantId));
        }
        register(new ParticipantAdded(participantId, name.value()));
        return Result.success(participantId);
    }

    public Result<ParticipantNotFound, ParticipantId> renameParticipant(
            ParticipantId participantId, ParticipantName name) {
        Participant participant = participants.get(participantId);
        if (participant == null || !participant.isActive()) {
            return Result.failure(new ParticipantNotFound(id, participantId));
        }
        if (!participant.isNamed(name)) {
            register(new ParticipantRenamed(participantId, name.value()));
        }
        return Result.success(participantId);
    }

    public Result<SettlementRejection, ParticipantId> removeParticipant(ParticipantId participantId) {
        Participant participant = participants.get(participantId);
        if (participant == null || !participant.isActive()) {
            return Result.failure(new ParticipantNotFound(id, participantId));
        }
        if (expenses.values().stream()
                .anyMatch(expense -> expense.payer().equals(participantId)
                        || expense.allocation().recipients().contains(participantId))) {
            return Result.failure(new ParticipantReferenced(id, participantId));
        }
        register(new ParticipantRemoved(participantId));
        return Result.success(participantId);
    }

    public Result<SettlementRejection, ExpenseId> recordExpense(
            ExpenseId expenseId,
            ExpenseDescription description,
            LocalDate incurredOn,
            ParticipantId payer,
            Money amount,
            ShareAllocation allocation) {
        ExpenseRecorded existing = expenses.get(expenseId);
        if (existing != null) {
            boolean identical = existing.description().equals(description)
                    && existing.incurredOn().equals(incurredOn)
                    && existing.payer().equals(payer)
                    && existing.allocation().equals(allocation)
                    && existing.originalAmount().currencyUnit().equals(amount.currencyUnit())
                    && existing.originalAmount().compareTo(amount) == 0;
            return identical ? Result.success(expenseId) : Result.failure(new ExpenseIdentifierConflict(id, expenseId));
        }
        if (amount.isZero() || amount.isNegative()) {
            return Result.failure(new NonPositiveExpenseAmount(id, expenseId));
        }
        if (allocation.recipients().isEmpty()) {
            return Result.failure(new EmptyShareAllocation(id, expenseId));
        }
        if (!active(payer)) {
            return Result.failure(new ParticipantNotFound(id, payer));
        }
        for (ParticipantId recipient : allocation.recipients()) {
            if (!active(recipient)) {
                return Result.failure(new ParticipantNotFound(id, recipient));
            }
        }
        if (!amount.currencyUnit().equals(currency)) {
            return Result.failure(new MissingExchangeRate(id, expenseId));
        }
        var valued = ValuationEngine.standard().value(amount, currency, incurredOn.atStartOfDay(), List.of());
        Money valuation = valued.money();
        register(new ExpenseRecorded(
                expenseId,
                description,
                incurredOn,
                payer,
                amount,
                allocation,
                valued.componentVersion().id(),
                valued.exchangeRate(),
                valuation,
                equalShares(valuation, allocation)));
        return Result.success(expenseId);
    }

    private boolean active(ParticipantId participantId) {
        Participant participant = participants.get(participantId);
        return participant != null && participant.isActive();
    }

    private static List<Share> equalShares(Money valuation, ShareAllocation allocation) {
        int count = allocation.recipients().size();
        Money[] division = valuation.divideAndRemainder(BigDecimal.valueOf(count));
        Money unit = valuation.smallestUnit();
        int residual = division[1].value().divideToIntegralValue(unit.value()).intValueExact();
        List<ParticipantId> priority = allocation.recipients().stream()
                .sorted(Comparator.comparing(ParticipantId::value))
                .toList();
        Map<ParticipantId, Money> resolved = new HashMap<>();
        for (ParticipantId recipient : allocation.recipients()) {
            resolved.put(recipient, division[0]);
        }
        for (int i = 0; i < residual; i++) {
            ParticipantId recipient = priority.get(i);
            resolved.put(recipient, resolved.get(recipient).add(unit));
        }
        List<Share> shares = new ArrayList<>();
        for (ParticipantId recipient : allocation.recipients()) {
            Money share = resolved.get(recipient);
            if (!share.isZero()) {
                shares.add(new Share(recipient, share));
            }
        }
        return List.copyOf(shares);
    }

    @Override
    public SettlementId id() {
        return id;
    }

    @Override
    protected void apply(SettlementEvent event) {
        switch (event) {
            case ExpenseRecorded recorded -> {
                if (currency == null
                        || expenses.containsKey(recorded.expenseId())
                        || !active(recorded.payer())
                        || recorded.allocation().recipients().stream().anyMatch(recipient -> !active(recipient))) {
                    throw new IllegalStateException("Invalid Expense recording");
                }
                expenses.put(recorded.expenseId(), recorded);
            }
            case SettlementOpened(var openedName, var openedCurrency) -> {
                if (currency != null) {
                    throw new IllegalStateException("Settlement already opened");
                }
                name = openedName;
                currency = openedCurrency;
            }
            case ParticipantAdded(var participantId, var participantName) -> {
                if (currency == null || participants.containsKey(participantId)) {
                    throw new IllegalStateException("Invalid Participant addition");
                }
                participants.put(participantId, new Participant(participantName));
            }
            case ParticipantRenamed(var participantId, var participantName) -> {
                if (currency == null || !participants.containsKey(participantId)) {
                    throw new IllegalStateException("Participant missing for rename");
                }
                participants.get(participantId).rename(participantName);
            }
            case ParticipantRemoved(var participantId) -> {
                if (currency == null
                        || !active(participantId)
                        || expenses.values().stream()
                                .anyMatch(expense -> expense.payer().equals(participantId)
                                        || expense.allocation().recipients().contains(participantId))) {
                    throw new IllegalStateException("Participant missing or referenced for removal");
                }
                participants.get(participantId).remove();
            }
            case SettlementRenamed(var newName) -> {
                if (currency == null) {
                    throw new IllegalStateException("Settlement opening missing");
                }
                name = newName;
            }
        }
    }
}
