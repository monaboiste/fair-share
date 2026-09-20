package com.github.monaboiste.fairshare.scoring.customer;

import com.github.monaboiste.fairshare.scoring.ast.Metric;
import java.util.Map;

public final class CustomerEventMetrics {

    public static final Metric AMOUNT = Metric.of("EVENT_AMOUNT");

    public static Metric typeIs(String type) {
        return Metric.of("EVENT_TYPE_IS_" + type);
    }

    public static Map<Metric, Double> of(CustomerEvent event) {
        return Map.of(AMOUNT, event.amount(), typeIs(event.type()), 1.0);
    }

    private CustomerEventMetrics() {}
}
