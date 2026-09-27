package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;

/**
 * Divides an Expense by exact amounts in the original Expense currency. Numerically equal amounts in the same currency
 * compare equal.
 */
public record ExactShareAllocation(SequencedMap<ParticipantId, Money> amounts) implements ShareAllocation {
    public ExactShareAllocation {
        amounts = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(amounts));
        amounts.forEach((recipient, amount) -> {
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(amount);
        });
    }

    @Override
    public SequencedSet<ParticipantId> recipients() {
        return amounts.sequencedKeySet();
    }

    @Override
    public Optional<SettlementRejection> validate(
            SettlementId settlementId, ExpenseId expenseId, Money originalAmount) {
        BigDecimal sum = BigDecimal.ZERO;
        for (var entry : amounts.entrySet()) {
            Money share = entry.getValue();
            if (!share.currencyUnit().equals(originalAmount.currencyUnit())) {
                return Optional.of(new ExactShareCurrencyMismatch(settlementId, expenseId, entry.getKey()));
            }
            if (share.isZero() || share.isNegative()) {
                return Optional.of(new NonPositiveExactShare(settlementId, expenseId, entry.getKey()));
            }
            sum = sum.add(share.value());
        }
        if (sum.compareTo(originalAmount.value()) != 0) {
            return Optional.of(new ExactShareSumMismatch(settlementId, expenseId));
        }
        return Optional.empty();
    }

    @Override
    public List<Share> resolve(Money amount) {
        if (amounts.isEmpty()) {
            throw new IllegalStateException("Cannot resolve invalid Share Allocation");
        }
        var sourceCurrency = amounts.firstEntry().getValue().currencyUnit();
        if (amounts.values().stream().anyMatch(share -> !share.currencyUnit().equals(sourceCurrency))) {
            throw new IllegalStateException("Cannot resolve invalid Share Allocation");
        }
        int scale = amounts.values().stream()
                .mapToInt(share -> share.value().scale())
                .max()
                .orElseThrow();
        SequencedMap<ParticipantId, BigInteger> proportions = new LinkedHashMap<>();
        amounts.forEach((recipient, share) -> proportions.put(
                recipient,
                share.value().setScale(scale, RoundingMode.UNNECESSARY).unscaledValue()));
        return LargestRemainderApportionment.apportion(proportions, amount);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ExactShareAllocation(SequencedMap<ParticipantId, Money> otherAmounts))
                || amounts.size() != otherAmounts.size()) {
            return false;
        }
        var left = amounts.entrySet().iterator();
        var right = otherAmounts.entrySet().iterator();
        while (left.hasNext()) {
            var first = left.next();
            var second = right.next();
            if (!first.getKey().equals(second.getKey())
                    || !first.getValue().currencyUnit().equals(second.getValue().currencyUnit())
                    || first.getValue().value().compareTo(second.getValue().value()) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (var entry : amounts.entrySet()) {
            BigDecimal amount = entry.getValue().value().stripTrailingZeros();
            hash = 31 * hash + Objects.hash(entry.getKey(), entry.getValue().currencyUnit(), amount);
        }
        return hash;
    }
}
