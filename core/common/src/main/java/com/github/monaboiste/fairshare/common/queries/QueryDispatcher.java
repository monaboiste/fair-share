package com.github.monaboiste.fairshare.common.queries;

import com.github.monaboiste.fairshare.common.Result;

public interface QueryDispatcher {
    <F, S> Result<F, S> dispatch(Query<F, S> query);
}
