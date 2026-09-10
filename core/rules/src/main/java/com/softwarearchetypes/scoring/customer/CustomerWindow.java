package com.softwarearchetypes.scoring.customer;

import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.EventWindow;
import com.softwarearchetypes.scoring.context.MetricOverlay;
import com.softwarearchetypes.scoring.context.MetricSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CustomerWindow implements EventWindow, MetricSource {

    private final String customerId;
    private final Instant from;
    private final Instant to;
    private final List<CustomerEvent> events;
    private final Map<Metric, Double> metrics;

    public CustomerWindow(
            String customerId, Instant from, Instant to, List<CustomerEvent> events, Map<Metric, Double> metrics) {
        this.customerId = customerId;
        this.from = from;
        this.to = to;
        this.events = List.copyOf(events);
        this.metrics = Map.copyOf(metrics);
    }

    public String getCustomerId() {
        return customerId;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public List<CustomerEvent> getEvents() {
        return events;
    }

    public Map<Metric, Double> getMetrics() {
        return metrics;
    }

    @Override
    public double metric(Metric metric) {
        return metrics.getOrDefault(metric, 0.0);
    }

    @Override
    public List<MetricSource> events() {
        return events.stream()
                .map(event -> (MetricSource) new MetricOverlay(CustomerEventMetrics.of(event), this))
                .toList();
    }
}
