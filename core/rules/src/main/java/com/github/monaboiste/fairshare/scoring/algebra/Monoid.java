package com.github.monaboiste.fairshare.scoring.algebra;

public interface Monoid<R> {

    R zero();

    R combine(R left, R right);
}
