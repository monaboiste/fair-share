package com.github.monaboiste.fairshare.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegisteredCommandDispatcher implements CommandDispatcher {
    private final Map<Class<?>, CommandHandler<?, ?>> handlers;

    public RegisteredCommandDispatcher(List<? extends CommandHandler<?, ?>> registered) {
        Map<Class<?>, CommandHandler<?, ?>> byType = new HashMap<>();
        for (CommandHandler<?, ?> handler : registered) {
            if (byType.putIfAbsent(handler.commandType(), handler) != null) {
                throw new IllegalArgumentException("Duplicate command handler: " + handler.commandType());
            }
        }
        handlers = Map.copyOf(byType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R dispatch(Command<R> command) {
        CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for command: " + command.getClass());
        }
        return handler.handle(command);
    }
}
