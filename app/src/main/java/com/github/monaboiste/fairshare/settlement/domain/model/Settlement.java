package com.github.monaboiste.fairshare.settlement.domain.model;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateFactory;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateRoot;
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.pricing.component.Validity;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.EmptyShareAllocation;
import com.github.monaboiste.fairshare.settlement.domain.ExchangeRateOverrideMismatch;
import com.github.monaboiste.fairshare.settlement.domain.ExchangeRateTargetMismatch;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDetails;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseIdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.ExplicitIdentityExchangeRate;
import com.github.monaboiste.fairshare.settlement.domain.MissingExchangeRate;
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveExpenseAmount;
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveRepaymentAmount;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantReferenced;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentAmountPrecisionExceeded;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentCurrencyMismatch;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentDetails;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentIdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.SelfDirectedRepayment;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.ShareAllocation;
import com.github.monaboiste.fairshare.settlement.domain.event.ExchangeRateConfigured;
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed;
import com.github.monaboiste.fairshare.settlement.domain.event.RepaymentRecorded;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import com.github.monaboiste.fairshare.valuation.ExchangeRate;
import com.github.monaboiste.fairshare.valuation.ExchangeRateOverride;
import com.github.monaboiste.fairshare.valuation.ExchangeRateVersion;
import com.github.monaboiste.fairshare.valuation.ExchangeRateVersions;
import com.github.monaboiste.fairshare.valuation.Valuation;
import com.github.monaboiste.fairshare.valuation.ValuationEngine;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public final class Settlement extends AggregateRoot<SettlementId, SettlementEvent> {
    private final SettlementId id;
    private @Nullable String name;
    private @Nullable CurrencyUnit currency;
    private final Map<ParticipantId, Participant> participants = new HashMap<>();
    private final Map<ExpenseId, ExpenseRecorded> expenses = new HashMap<>();
    private final Map<RepaymentId, RepaymentRecorded> repayments = new HashMap<>();
    private final ExchangeRateVersions exchangeRates = ExchangeRateVersions.empty();

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
        if (isReferenced(participantId)) {
            return Result.failure(new ParticipantReferenced(id, participantId));
        }
        register(new ParticipantRemoved(participantId));
        return Result.success(participantId);
    }

    private boolean isReferenced(ParticipantId participantId) {
        return expenses.values().stream()
                        .anyMatch(expense -> expense.payer().equals(participantId)
                                || expense.allocation().recipients().contains(participantId))
                || repayments.values().stream()
                        .anyMatch(repayment -> repayment.payer().equals(participantId)
                                || repayment.recipient().equals(participantId));
    }

    public Result<SettlementRejection, ComponentVersionId> configureExchangeRate(
            ExchangeRate exchangeRate, Validity validity, ComponentVersionId versionId, LocalDateTime definedAt) {
        if (!exchangeRate.targetCurrency().equals(settlementCurrency())) {
            return Result.failure(new ExchangeRateTargetMismatch(id, exchangeRate.targetCurrency()));
        }
        if (exchangeRate.sourceCurrency().equals(exchangeRate.targetCurrency())) {
            return Result.failure(new ExplicitIdentityExchangeRate(id));
        }
        Optional<ExchangeRateVersion> previous =
                exchangeRates.latestFor(exchangeRate.sourceCurrency(), exchangeRate.targetCurrency(), validity);
        if (previous.isPresent() && previous.get().exchangeRate().value().compareTo(exchangeRate.value()) == 0) {
            return Result.success(previous.get().id());
        }
        ExchangeRateVersion version = new ExchangeRateVersion(versionId, exchangeRate, validity, definedAt);
        register(new ExchangeRateConfigured(version));
        return Result.success(version.id());
    }

    public Result<SettlementRejection, ExpenseId> recordExpense(
            ExpenseDetails expense, @Nullable ExchangeRateOverride override, ValuationEngine engine) {
        ExchangeRate overrideRate = override == null ? null : override.rate();
        ExpenseRecorded existing = expenses.get(expense.expenseId());
        if (existing != null) {
            return retry(existing, expense, overrideRate);
        }
        Optional<SettlementRejection> rejection = validateNewExpense(expense, overrideRate);
        if (rejection.isPresent()) {
            return Result.failure(rejection.get());
        }
        Result<SettlementRejection, Valuation> valued = value(expense, override, engine);
        if (valued.failure()) {
            return Result.failure(valued.getFailure());
        }
        registerExpense(expense, overrideRate, valued.getSuccess());
        return Result.success(expense.expenseId());
    }

    public Result<SettlementRejection, RepaymentId> recordRepayment(RepaymentDetails repayment) {
        if (repayments.containsKey(repayment.repaymentId())) {
            return Result.failure(new RepaymentIdentifierConflict(id, repayment.repaymentId()));
        }
        if (repayment.amount().isZero() || repayment.amount().isNegative()) {
            return Result.failure(new NonPositiveRepaymentAmount(id, repayment.repaymentId()));
        }
        if (!repayment.amount().currencyUnit().equals(settlementCurrency())) {
            return Result.failure(new RepaymentCurrencyMismatch(id, repayment.repaymentId()));
        }
        if (repayment.amount().value().stripTrailingZeros().scale()
                > settlementCurrency().getDefaultFractionDigits()) {
            return Result.failure(new RepaymentAmountPrecisionExceeded(id, repayment.repaymentId()));
        }
        if (repayment.payer().equals(repayment.recipient())) {
            return Result.failure(new SelfDirectedRepayment(id, repayment.repaymentId()));
        }
        if (!active(repayment.payer())) {
            return Result.failure(new ParticipantNotFound(id, repayment.payer()));
        }
        if (!active(repayment.recipient())) {
            return Result.failure(new ParticipantNotFound(id, repayment.recipient()));
        }
        register(new RepaymentRecorded(
                repayment.repaymentId(),
                repayment.paidOn(),
                repayment.payer(),
                repayment.recipient(),
                repayment.amount()));
        return Result.success(repayment.repaymentId());
    }

    private Result<SettlementRejection, ExpenseId> retry(
            ExpenseRecorded existing, ExpenseDetails expense, @Nullable ExchangeRate overrideRate) {
        boolean identical = existing.description().equals(expense.description())
                && existing.incurredOn().equals(expense.incurredOn())
                && existing.payer().equals(expense.payer())
                && existing.allocation().equals(expense.allocation())
                && existing.originalAmount()
                        .currencyUnit()
                        .equals(expense.amount().currencyUnit())
                && existing.originalAmount().compareTo(expense.amount()) == 0
                && sameOverride(existing.exchangeRateOverride(), overrideRate);
        return identical
                ? Result.success(expense.expenseId())
                : Result.failure(new ExpenseIdentifierConflict(id, expense.expenseId()));
    }

    private static boolean sameOverride(@Nullable ExchangeRate recorded, @Nullable ExchangeRate requested) {
        if (recorded == null || requested == null) {
            return recorded == null && requested == null;
        }
        return recorded.sourceCurrency().equals(requested.sourceCurrency())
                && recorded.targetCurrency().equals(requested.targetCurrency())
                && recorded.value().compareTo(requested.value()) == 0;
    }

    private Optional<SettlementRejection> validateNewExpense(
            ExpenseDetails expense, @Nullable ExchangeRate overrideRate) {
        ExpenseId expenseId = expense.expenseId();
        Money amount = expense.amount();
        ShareAllocation allocation = expense.allocation();
        if (amount.isZero() || amount.isNegative()) {
            return Optional.of(new NonPositiveExpenseAmount(id, expenseId));
        }
        if (allocation.recipients().isEmpty()) {
            return Optional.of(new EmptyShareAllocation(id, expenseId));
        }
        var invalidAllocation = allocation.validate(id, expenseId, amount);
        if (invalidAllocation.isPresent()) {
            return invalidAllocation;
        }
        if (!active(expense.payer())) {
            return Optional.of(new ParticipantNotFound(id, expense.payer()));
        }
        for (ParticipantId recipient : allocation.recipients()) {
            if (!active(recipient)) {
                return Optional.of(new ParticipantNotFound(id, recipient));
            }
        }
        if (overrideRate != null && !convertsIntoSettlementCurrency(overrideRate, amount)) {
            return Optional.of(new ExchangeRateOverrideMismatch(id, expenseId));
        }
        return Optional.empty();
    }

    private boolean convertsIntoSettlementCurrency(ExchangeRate rate, Money amount) {
        return rate.sourceCurrency().equals(amount.currencyUnit())
                && rate.targetCurrency().equals(settlementCurrency())
                && !amount.currencyUnit().equals(settlementCurrency());
    }

    private Result<SettlementRejection, Valuation> value(
            ExpenseDetails expense, @Nullable ExchangeRateOverride override, ValuationEngine engine) {
        Money amount = expense.amount();
        if (override != null) {
            return Result.success(engine.value(amount, settlementCurrency(), override));
        }
        if (amount.currencyUnit().equals(settlementCurrency())) {
            return Result.success(engine.identity(amount));
        }
        Optional<ExchangeRateVersion> selected = exchangeRates.applicableAt(
                amount.currencyUnit(),
                settlementCurrency(),
                expense.incurredOn().atStartOfDay());
        if (selected.isEmpty()) {
            return Result.failure(new MissingExchangeRate(id, expense.expenseId()));
        }
        return Result.success(engine.value(amount, settlementCurrency(), selected.get()));
    }

    private void registerExpense(ExpenseDetails expense, @Nullable ExchangeRate override, Valuation valued) {
        Money valuation = valued.money();
        register(new ExpenseRecorded(
                expense.expenseId(),
                expense.description(),
                expense.incurredOn(),
                expense.payer(),
                expense.amount(),
                expense.allocation(),
                override,
                valued.componentVersion().id(),
                valued.exchangeRate(),
                valuation,
                expense.allocation().resolve(valuation)));
    }

    private CurrencyUnit settlementCurrency() {
        CurrencyUnit openedCurrency = currency;
        if (openedCurrency == null) {
            throw new IllegalStateException("Settlement opening missing");
        }
        return openedCurrency;
    }

    private boolean active(ParticipantId participantId) {
        Participant participant = participants.get(participantId);
        return participant != null && participant.isActive();
    }

    @Override
    public SettlementId id() {
        return id;
    }

    @Override
    protected void apply(SettlementEvent event) {
        switch (event) {
            case ExpenseRecorded recorded -> applyExpenseRecorded(recorded);
            case RepaymentRecorded recorded -> applyRepaymentRecorded(recorded);
            case ExchangeRateConfigured configured -> applyExchangeRateConfigured(configured);
            case SettlementOpened(var openedName, var openedCurrency) ->
                applySettlementOpened(openedName, openedCurrency);
            case ParticipantAdded(var participantId, var participantName) ->
                applyParticipantAdded(participantId, participantName);
            case ParticipantRenamed(var participantId, var participantName) ->
                applyParticipantRenamed(participantId, participantName);
            case ParticipantRemoved(var participantId) -> applyParticipantRemoved(participantId);
            case SettlementRenamed(var newName) -> applySettlementRenamed(newName);
        }
    }

    private void applyExchangeRateConfigured(ExchangeRateConfigured configured) {
        if (currency == null) {
            throw new IllegalStateException("Settlement opening missing");
        }
        exchangeRates.append(configured.version());
    }

    private void applySettlementRenamed(String newName) {
        if (currency == null) {
            throw new IllegalStateException("Settlement opening missing");
        }
        name = newName;
    }

    private void applyParticipantRemoved(ParticipantId participantId) {
        if (currency == null || !active(participantId) || isReferenced(participantId)) {
            throw new IllegalStateException("Participant missing or referenced for removal");
        }
        participants.get(participantId).remove();
    }

    private void applyParticipantRenamed(ParticipantId participantId, String participantName) {
        if (currency == null || !participants.containsKey(participantId)) {
            throw new IllegalStateException("Participant missing for rename");
        }
        participants.get(participantId).rename(participantName);
    }

    private void applyParticipantAdded(ParticipantId participantId, String participantName) {
        if (currency == null || participants.containsKey(participantId)) {
            throw new IllegalStateException("Invalid Participant addition");
        }
        participants.put(participantId, new Participant(participantName));
    }

    private void applySettlementOpened(String openedName, CurrencyUnit openedCurrency) {
        if (currency != null) {
            throw new IllegalStateException("Settlement already opened");
        }
        name = openedName;
        currency = openedCurrency;
    }

    private void applyExpenseRecorded(ExpenseRecorded recorded) {
        if (currency == null
                || expenses.containsKey(recorded.expenseId())
                || !active(recorded.payer())
                || anyRecipientInactive(recorded)) {
            throw new IllegalStateException("Invalid Expense recording");
        }
        expenses.put(recorded.expenseId(), recorded);
    }

    private void applyRepaymentRecorded(RepaymentRecorded recorded) {
        if (currency == null
                || repayments.containsKey(recorded.repaymentId())
                || recorded.payer().equals(recorded.recipient())
                || !active(recorded.payer())
                || !active(recorded.recipient())) {
            throw new IllegalStateException("Invalid Repayment recording");
        }
        repayments.put(recorded.repaymentId(), recorded);
    }

    private boolean anyRecipientInactive(ExpenseRecorded recorded) {
        return recorded.allocation().recipients().stream().anyMatch(recipient -> !active(recipient));
    }
}
