package com.softwarearchetypes.scoring.algebra;

public interface Monoid<R> {

    R zero();

    R combine(R left, R right);
}
