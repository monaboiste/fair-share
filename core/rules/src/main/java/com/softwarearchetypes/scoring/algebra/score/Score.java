package com.softwarearchetypes.scoring.algebra.score;

public record Score(int value) {

    public static final Score ZERO = new Score(0);

    public Score plus(Score other) {
        return new Score(this.value + other.value);
    }

    public Score minus(Score other) {
        return new Score(this.value - other.value);
    }
}
