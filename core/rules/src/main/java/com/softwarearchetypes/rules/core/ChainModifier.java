package com.softwarearchetypes.rules.core;

import java.util.ArrayList;
import java.util.List;

public class ChainModifier<T> implements Modifier<T> {

    private final List<Modifier<T>> modifiers = new ArrayList<>();

    @Override
    public T modify(T subject) {
        for (Modifier<T> modifier : modifiers) {
            subject = modifier.modify(subject);
        }
        return subject;
    }

    public ChainModifier<T> add(Modifier<T> modifier) {
        modifiers.add(modifier);
        return this;
    }
}
