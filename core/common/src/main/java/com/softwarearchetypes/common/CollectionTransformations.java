package com.softwarearchetypes.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class CollectionTransformations {

    private CollectionTransformations() {}

    public static Map<String, String> keyValueMapFrom(String[] parameters) {
        if (parameters == null) {
            return new HashMap<>();
        }
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException("The number of arguments must be even (key, productName, ...)");
        }
        return IntStream.range(0, parameters.length / 2)
                .map(i -> i * 2)
                .boxed()
                .collect(Collectors.toMap(
                        i -> requireValidKey(parameters[i], i), i -> parameters[i + 1], (_, rhs) -> rhs, HashMap::new));
    }

    private static String requireValidKey(String key, int index) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key [idx: %d] cannot be empty or null".formatted(index));
        }
        return key;
    }

    public static <T> Set<T> subtract(Set<T> minuend, Set<T> subtrahend) {
        Set<T> result = new HashSet<>(minuend);
        result.removeAll(subtrahend);
        return result;
    }
}
