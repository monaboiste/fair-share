package com.github.monaboiste.fairshare.common.queries;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.internal.HandlerRegistry;
import java.util.Map;

public final class RegisteredQueryDispatcher implements QueryDispatcher {
    private final Map<Class<?>, QueryHandler<?, ?, ?>> handlers;

    private RegisteredQueryDispatcher(Map<Class<?>, QueryHandler<?, ?, ?>> handlers) {
        this.handlers = handlers;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <F, S> Result<F, S> dispatch(Query<F, S> query) {
        @SuppressWarnings("unchecked")
        QueryHandler<Query<F, S>, F, S> handler = (QueryHandler<Query<F, S>, F, S>) handlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for query: " + query.getClass());
        }
        return handler.handle(query);
    }

    public static final class Builder {
        private final HandlerRegistry<QueryHandler<?, ?, ?>> handlers = new HandlerRegistry<>();

        public <Q extends Query<F, S>, F, S> Builder register(Class<Q> type, QueryHandler<Q, F, S> handler) {
            handlers.register(type, handler);
            return this;
        }

        public Builder requireHandlersFor(Class<?> sealedRoot) {
            handlers.requireHandlersFor(sealedRoot);
            return this;
        }

        public RegisteredQueryDispatcher build() {
            return new RegisteredQueryDispatcher(handlers.build());
        }
    }
}
