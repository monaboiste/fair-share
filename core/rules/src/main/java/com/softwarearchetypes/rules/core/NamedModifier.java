package com.softwarearchetypes.rules.core;

public abstract class NamedModifier<T> implements Modifier<T> {

    private final String name;

    protected NamedModifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
