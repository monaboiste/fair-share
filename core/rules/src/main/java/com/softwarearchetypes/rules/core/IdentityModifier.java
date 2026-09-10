package com.softwarearchetypes.rules.core;

public final class IdentityModifier<T> implements Modifier<T> {

    @Override
    public T modify(T subject) {
        return subject;
    }
}
