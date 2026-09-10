package com.softwarearchetypes.scoring.algebra.score;

import com.softwarearchetypes.scoring.algebra.Monoid;

public class ScoreMonoid implements Monoid<Score> {

    @Override
    public Score zero() {
        return Score.ZERO;
    }

    @Override
    public Score combine(Score left, Score right) {
        return left.plus(right);
    }
}
