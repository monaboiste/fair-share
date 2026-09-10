package com.softwarearchetypes.scoring.algebra.explained;

import java.util.Collections;
import java.util.List;

public record ExplainedScore(int total, List<Contribution> contributions) {

    public ExplainedScore(int total, List<Contribution> contributions) {
        this.total = total;
        this.contributions = List.copyOf(contributions);
    }

    @Override
    public List<Contribution> contributions() {
        return Collections.unmodifiableList(contributions);
    }

    public ExplainedScore plus(ExplainedScore other) {
        int newTotal = this.total + other.total;
        List<Contribution> merged = new java.util.ArrayList<>(this.contributions);
        merged.addAll(other.contributions);
        return new ExplainedScore(newTotal, merged);
    }
}
