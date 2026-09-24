package com.github.monaboiste.fairshare.common.queries;

public interface QueryDispatcher {
    <R> R dispatch(Query<R> query);
}
