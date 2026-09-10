package com.softwarearchetypes.rules.core;

public interface Modifier<T> {

    T modify(T subject);
}
