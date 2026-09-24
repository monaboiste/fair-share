package com.github.monaboiste.fairshare.common.queries;

import com.github.monaboiste.fairshare.common.Result;

@FunctionalInterface
public interface QueryHandler<Q extends Query<F, S>, F, S> {
    Result<F, S> handle(Q query);
}
