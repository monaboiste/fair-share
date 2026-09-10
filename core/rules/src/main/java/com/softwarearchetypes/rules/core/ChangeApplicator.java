package com.softwarearchetypes.rules.core;

public interface ChangeApplicator<T, V> {

    V currentValue(T subject);

    T applyChange(T subject, V newValue, String description);
}
