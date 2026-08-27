package com.softwarearchetypes.common;

public final class Preconditions {

    private Preconditions() {}

    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void checkState(boolean state, String errorMessage) {
        if (!state) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static void checkNotNull(Object value, String errorMessage) {
        checkArgument(value != null, errorMessage);
    }

    public static void checkNotBlank(String value, String errorMessage) {
        checkArgument(value != null && !value.isBlank(), errorMessage);
    }
}
