package com.github.monaboiste.fairshare.scoring.algebra.explained;

import com.github.monaboiste.fairshare.scoring.algebra.Monoid;
import java.util.List;

public class ExplainedScoreMonoid implements Monoid<ExplainedScore> {

    @Override
    public ExplainedScore zero() {
        return new ExplainedScore(0, List.of());
    }

    @Override
    public ExplainedScore combine(ExplainedScore left, ExplainedScore right) {
        return left.plus(right);
    }
}
