package com.github.monaboiste.fairshare.scoring.context;

import com.github.monaboiste.fairshare.scoring.ast.Metric;

@FunctionalInterface
public interface MetricSource {

    double metric(Metric metric);
}
