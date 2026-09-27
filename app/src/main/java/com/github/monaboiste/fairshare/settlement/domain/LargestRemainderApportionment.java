package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

final class LargestRemainderApportionment {
    private LargestRemainderApportionment() {}

    /**
     * Apportions a Valuation in minor units by positive integer proportions: floors first, then largest remainders with
     * Participant identifier ties. Omits zero Shares, retains presentation order, and fails fast on invalid input.
     */
    static List<Share> apportion(SequencedMap<ParticipantId, BigInteger> proportions, Money amount) {
        if (proportions.isEmpty() || proportions.values().stream().anyMatch(proportion -> proportion.signum() <= 0)) {
            throw new IllegalStateException("Cannot resolve invalid Share Allocation");
        }
        BigInteger units = amount.value()
                .divide(amount.smallestUnit().value(), 0, RoundingMode.UNNECESSARY)
                .toBigIntegerExact();
        if (units.signum() < 0) {
            throw new IllegalStateException("Cannot resolve negative Valuation");
        }
        BigInteger total = proportions.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
        Map<ParticipantId, BigInteger> floors = new HashMap<>();
        Map<ParticipantId, BigInteger> remainders = new HashMap<>();
        BigInteger assigned = BigInteger.ZERO;
        for (var entry : proportions.entrySet()) {
            BigInteger[] quotient = units.multiply(entry.getValue()).divideAndRemainder(total);
            floors.put(entry.getKey(), quotient[0]);
            remainders.put(entry.getKey(), quotient[1]);
            assigned = assigned.add(quotient[0]);
        }
        List<ParticipantId> priority = proportions.keySet().stream()
                .sorted(Comparator.<ParticipantId, BigInteger>comparing(remainders::get)
                        .reversed()
                        .thenComparing(ParticipantId::value))
                .toList();
        int residual = units.subtract(assigned).intValueExact();
        for (int i = 0; i < residual; i++) {
            ParticipantId recipient = priority.get(i);
            floors.put(recipient, floors.get(recipient).add(BigInteger.ONE));
        }
        List<Share> shares = new ArrayList<>();
        for (ParticipantId recipient : proportions.keySet()) {
            BigInteger count = floors.get(recipient);
            if (count.signum() != 0) {
                shares.add(new Share(
                        recipient,
                        Money.of(
                                new BigDecimal(count)
                                        .multiply(amount.smallestUnit().value()),
                                amount.currency())));
            }
        }
        return List.copyOf(shares);
    }
}
