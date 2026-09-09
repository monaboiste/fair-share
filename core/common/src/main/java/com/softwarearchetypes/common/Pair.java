package com.softwarearchetypes.common;

import org.jspecify.annotations.Nullable;

public record Pair<T extends @Nullable Object>(T first, T second) {

    public static <T extends @Nullable Object> Pair<T> of(T first, T second) {
        return new Pair<>(first, second);
    }
}
