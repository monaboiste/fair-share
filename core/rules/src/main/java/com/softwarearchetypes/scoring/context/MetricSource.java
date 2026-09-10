package com.softwarearchetypes.scoring.context;

import com.softwarearchetypes.scoring.ast.Metric;

@FunctionalInterface
public interface MetricSource {

    double metric(Metric metric);
}
