package com.softwarearchetypes.common;

public final class StringUtils {

    private StringUtils() {}

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
