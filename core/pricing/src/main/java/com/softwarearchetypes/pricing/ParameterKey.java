package com.softwarearchetypes.pricing;

public record ParameterKey<T>(String name, Class<T> type) implements ParameterDefinition {
    @Override
    public String expectedType() {
        return type.getSimpleName();
    }
}
