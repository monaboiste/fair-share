package com.github.monaboiste.fairshare.common.internal;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HandlerRegistry<H> {
    private final Map<Class<?>, H> handlers = new HashMap<>();
    private final Set<Class<?>> required = new HashSet<>();

    public void register(Class<?> type, H handler) {
        if (handlers.putIfAbsent(type, handler) != null) {
            throw new IllegalArgumentException("Duplicate handler: " + type);
        }
    }

    public void requireHandlersFor(Class<?> sealedRoot) {
        if (!sealedRoot.isSealed()) {
            throw new IllegalArgumentException("Not sealed: " + sealedRoot);
        }
        collect(sealedRoot);
    }

    private void collect(Class<?> type) {
        if (type.isSealed()) {
            for (Class<?> permitted : type.getPermittedSubclasses()) {
                collect(permitted);
            }
        } else if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            required.add(type);
        }
    }

    public Map<Class<?>, H> build() {
        if (!handlers.keySet().containsAll(required)) {
            Set<Class<?>> missing = new HashSet<>(required);
            missing.removeAll(handlers.keySet());
            throw new IllegalStateException("Missing handlers: " + missing);
        }
        return Map.copyOf(handlers);
    }
}
