package com.github.monaboiste.fairshare.common.queries;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
