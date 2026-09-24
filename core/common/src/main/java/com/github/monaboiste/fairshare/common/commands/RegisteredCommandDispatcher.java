package com.github.monaboiste.fairshare.common.commands;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.internal.HandlerRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RegisteredCommandDispatcher implements CommandDispatcher {
    private final Map<Class<?>, CommandHandler<?, ?, ?>> handlers;
    private final CommandInterceptor.Proceed chain;

    private RegisteredCommandDispatcher(
            Map<Class<?>, CommandHandler<?, ?, ?>> handlers, List<CommandInterceptor> interceptors) {
        this.handlers = handlers;
        CommandInterceptor.Proceed next = this::handle;
        for (int index = interceptors.size() - 1; index >= 0; index--) {
            CommandInterceptor interceptor = interceptors.get(index);
            CommandInterceptor.Proceed downstream = next;
            next = new CommandInterceptor.Proceed() {
                @Override
                public <F extends CommandFailure, S> Result<F, S> handle(Command<F, S> command) {
                    return interceptor.intercept(command, downstream);
                }
            };
        }
        chain = next;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <F extends CommandFailure, S> Result<F, S> dispatch(Command<F, S> command) {
        return chain.handle(command);
    }

    private <F extends CommandFailure, S> Result<F, S> handle(Command<F, S> command) {
        @SuppressWarnings("unchecked")
        CommandHandler<Command<F, S>, F, S> handler =
                (CommandHandler<Command<F, S>, F, S>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for command: " + command.getClass());
        }
        return handler.handle(command);
    }

    public static final class Builder {
        private final HandlerRegistry<CommandHandler<?, ?, ?>> handlers = new HandlerRegistry<>();
        private final List<CommandInterceptor> interceptors = new ArrayList<>();

        public <C extends Command<F, S>, F extends CommandFailure, S> Builder register(
                Class<C> type, CommandHandler<C, F, S> handler) {
            handlers.register(type, handler);
            return this;
        }

        public Builder intercept(CommandInterceptor interceptor) {
            interceptors.add(interceptor);
            return this;
        }

        public Builder requireHandlersFor(Class<?> sealedRoot) {
            handlers.requireHandlersFor(sealedRoot);
            return this;
        }

        public RegisteredCommandDispatcher build() {
            return new RegisteredCommandDispatcher(handlers.build(), List.copyOf(interceptors));
        }
    }
}
