package com.softwarearchetypes.pricing;

public sealed interface ParameterDefinition permits ParameterKey {
    String name();

    String expectedType();
}
