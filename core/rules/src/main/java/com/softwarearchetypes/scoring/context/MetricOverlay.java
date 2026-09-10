package com.softwarearchetypes.scoring.context;

import com.softwarearchetypes.scoring.ast.Metric;
import java.util.Map;

public record MetricOverlay(Map<Metric, Double> extra, MetricSource fallback) implements MetricSource {

    public MetricOverlay {
        extra = Map.copyOf(extra);
    }

    @Override
    public double metric(Metric metric) {
        Double value = extra.get(metric);
        return value != null ? value : fallback.metric(metric);
    }
}
